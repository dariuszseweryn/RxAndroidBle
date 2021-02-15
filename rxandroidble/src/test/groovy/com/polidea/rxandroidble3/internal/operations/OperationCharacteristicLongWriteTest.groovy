package com.polidea.rxandroidble3.internal.operations
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.polidea.rxandroidble3.internal.connection.NoRetryStrategy
import com.polidea.rxandroidble3.RxBleConnection
import com.polidea.rxandroidble3.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble3.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble3.exceptions.BleGattOperationType
import com.polidea.rxandroidble3.internal.connection.ImmediateSerializedBatchAckStrategy
import com.polidea.rxandroidble3.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble3.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble3.internal.util.ByteAssociation
import com.polidea.rxandroidble3.internal.util.MockOperationTimeoutConfiguration
import com.polidea.rxandroidble3.internal.util.QueueReleasingEmitterWrapper
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableSource
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicReference
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class OperationCharacteristicLongWriteTest extends Specification {

    private static long DEFAULT_WRITE_DELAY = 1
    UUID mockCharacteristicUUID = UUID.randomUUID()
    UUID differentCharacteristicUUID = UUID.randomUUID()
    BluetoothGatt mockGatt = Mock BluetoothGatt
    BluetoothDevice mockDevice = Mock BluetoothDevice
    RxBleGattCallback mockCallback = Mock RxBleGattCallback
    BluetoothGattCharacteristic mockCharacteristic = Mock BluetoothGattCharacteristic
    RxBleConnection.WriteOperationAckStrategy writeOperationAckStrategy
    RxBleConnection.WriteOperationRetryStrategy writeOperationRetryStrategy
    TestScheduler testScheduler = new TestScheduler()
    TestScheduler timeoutScheduler = new TestScheduler()
    Scheduler immediateScheduler = Schedulers.trampoline()
    PublishSubject<ByteAssociation<UUID>> onCharacteristicWriteSubject = PublishSubject.create()
    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface
    CharacteristicLongWriteOperation objectUnderTest
    @Shared
    Exception testException = new Exception("testException")

    def setup() {
        mockCharacteristic.getUuid() >> mockCharacteristicUUID
        mockCallback.getOnCharacteristicWrite() >> onCharacteristicWriteSubject
        mockGatt.getDevice() >> mockDevice
        mockDevice.getAddress() >> "test"
    }

    def "should call BluetoothGattCharacteristic.setValue() before calling BluetoothGatt.writeCharacteristic()"() {

        given:
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        byte[] writtenBytes = byteArray(20)
        prepareObjectUnderTest(20, writtenBytes)

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockCharacteristic.setValue(writtenBytes) >> true

        then:
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> true
    }

    @Unroll
    def "should not call BluetoothGattCharacteristic.setValue() with proper parts not longer than maxBatchSize"() {

        given:
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        givenEachCharacteristicWriteOkAfterDefaultDelay()
        byte[] writtenBytes = byteArray(writtenBytesLength)
        byte[][] expectedBatches = expectedBatches(writtenBytes, maxBatchSize)
        AtomicInteger batch = new AtomicInteger(0)
        prepareObjectUnderTest(maxBatchSize, writtenBytes)

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()
        advanceTimeForWrites(expectedBatchesCount)

        then:
        expectedBatchesCount * mockCharacteristic.setValue({ byte[] bytes ->
            int batchIndex = batch.getAndIncrement()
            def expectedBatch = expectedBatches[batchIndex]
            bytes.length <= maxBatchSize && bytes == expectedBatch
        }) >> true

        where:
        maxBatchSize | writtenBytesLength | expectedBatchesCount
        20           | 17                 | 1
        20           | 57                 | 3
        4            | 16                 | 4
        4            | 17                 | 5
        1000         | 46532              | 47
    }

    def "asObservable() should not emit error when BluetoothGatt.writeCharacteristic() returns true every time"() {

        given:
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        givenEachCharacteristicWriteOkAfterDefaultDelay()
        prepareObjectUnderTest(20, byteArray(60))

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()
        advanceTimeForWrites(3)

        then:
        testSubscriber.assertNoErrors()
    }

    @Unroll
    def "asObservable() should emit error when BluetoothGatt.writeCharacteristic() returns false at any time"() {

        given:
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        givenCharacteristicWriteOkButEventuallyFailsToStart(failingWriteIndex)
        prepareObjectUnderTest(20, byteArray(60))

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()
        advanceTimeForWrites(failingWriteIndex)

        then:
        testSubscriber.assertError(BleGattCannotStartException)

        and:
        testSubscriber.assertError({ exception ->
            ((BleGattCannotStartException) exception).bleGattOperationType == BleGattOperationType.CHARACTERISTIC_LONG_WRITE
        })

        where:
        failingWriteIndex << [0, 1, 2]
    }

    @Unroll
    def "asObservable() should emit error when BluetoothGatt.onCharacteristicWrite() emits error at any time"() {

        given:
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        givenCharacteristicWriteOkButEventuallyFails(failingWriteIndex)
        prepareObjectUnderTest(20, byteArray(60))

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()
        advanceTimeForWritesToComplete(failingWriteIndex)

        then:
        testSubscriber.assertError(testException)

        where:
        failingWriteIndex << [0, 1, 2]
    }

    def "should not subscribe to RxBleGattCallback.onCharacteristicWrite() before run()"() {

        given:
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        mockCallback = Mock RxBleGattCallback

        when:
        prepareObjectUnderTest(20, byteArray(60))

        then:
        0 * mockCallback.getOnCharacteristicWrite() >> Observable.empty()
    }

    def "should complete part of the write if RxBleGattCallback.onCharacteristicWrite() will be called with proper characteristic"() {

        given:
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        givenCharacteristicWriteStartsOkButDifferentCharacteristicOnCallbackFirst()
        prepareObjectUnderTest(20, byteArray(60))

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()
        advanceTimeForWrites(1)

        then:
        1 * mockCharacteristic.setValue(_) >> true

        when:
        advanceTimeForWrites(1)

        then:
        1 * mockCharacteristic.setValue(_) >> true
    }

    def "should attempt to write next batch after the previous has completed and has been acknowledged - no retry strategy"() {
        given:
        this.writeOperationRetryStrategy = new NoRetryStrategy()
        AcknowledgementTrigger writeAckTrigger = givenWillTriggerWriteAcknowledgement()
        givenEachCharacteristicWriteOkAfterDefaultDelay()
        prepareObjectUnderTest(2, [0x1, 0x1, 0x2, 0x2, 0x3, 0x3] as byte[])

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()
        advanceTimeForWritesToComplete(1)

        then:
        1 * mockCharacteristic.setValue([0x1, 0x1] as byte[]) >> true

        when:
        writeAckTrigger.acknowledgeWrite()

        then:
        1 * mockCharacteristic.setValue([0x2, 0x2] as byte[]) >> true
    }

    def "should not attempt to write next batch or rewrite after the previous has failed - no retry strategy"() {
        given:
        this.writeOperationRetryStrategy = new NoRetryStrategy()
        this.writeOperationAckStrategy = new ImmediateSerializedBatchAckStrategy()
        prepareObjectUnderTest(2, [0x1, 0x1, 0x2, 0x2, 0x3, 0x3] as byte[])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockCharacteristic.setValue([0x1, 0x1] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> false
        testSubscriber.assertError(BleGattCannotStartException)
    }

    def "attempt to rewrite the failed batch if the strategy has emitted the LongWriteFailure - first batch"() {
        given:
        RetryWriteOperation retryWriteOperationStrategy = givenWillRetryWriteOperation()
        this.writeOperationRetryStrategy = retryWriteOperationStrategy
        this.writeOperationAckStrategy = new ImmediateSerializedBatchAckStrategy()
        prepareObjectUnderTest(2, [0x1, 0x1, 0x2, 0x2, 0x3, 0x3] as byte[])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockCharacteristic.setValue([0x1, 0x1] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> false
        testSubscriber.assertNotTerminated()

        when:
        retryWriteOperationStrategy.triggerRetry()

        then:
        1 * mockCharacteristic.setValue([0x1, 0x1] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), [] as byte[]))
            true
        }

        then:
        1 * mockCharacteristic.setValue([0x2, 0x2] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), [] as byte[]))
            true
        }

        then:
        1 * mockCharacteristic.setValue([0x3, 0x3] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), [] as byte[]))
            true
        }

        testSubscriber.assertValuesEquals([[0x1, 0x1, 0x2, 0x2, 0x3, 0x3]] as byte[][])
        testSubscriber.assertComplete()
    }

    def "attempt to rewrite the failed batch if the strategy has emitted the LongWriteFailure - mid batch"() {
        given:
        RetryWriteOperation retryWriteOperationStrategy = givenWillRetryWriteOperation()
        this.writeOperationRetryStrategy = retryWriteOperationStrategy
        this.writeOperationAckStrategy = new ImmediateSerializedBatchAckStrategy()
        prepareObjectUnderTest(2, [0x1, 0x1, 0x2, 0x2, 0x3, 0x3] as byte[])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockCharacteristic.setValue([0x1, 0x1] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), [] as byte[]))
            true
        }

        then:
        1 * mockCharacteristic.setValue([0x2, 0x2] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> false

        when:
        retryWriteOperationStrategy.triggerRetry()

        then:
        1 * mockCharacteristic.setValue([0x2, 0x2] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), [] as byte[]))
            true
        }

        then:
        1 * mockCharacteristic.setValue([0x3, 0x3] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), [] as byte[]))
            true
        }

        testSubscriber.assertValuesEquals([[0x1, 0x1, 0x2, 0x2, 0x3, 0x3]] as byte[][])
        testSubscriber.assertComplete()
    }

    def "attempt to rewrite the failed batch if the strategy has emitted the LongWriteFailure - last batch"() {
        given:
        RetryWriteOperation retryWriteOperationStrategy = givenWillRetryWriteOperation()
        this.writeOperationRetryStrategy = retryWriteOperationStrategy
        this.writeOperationAckStrategy = new ImmediateSerializedBatchAckStrategy()
        prepareObjectUnderTest(2, [0x1, 0x1, 0x2, 0x2, 0x3, 0x3] as byte[])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockCharacteristic.setValue([0x1, 0x1] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), [] as byte[]))
            true
        }

        then:
        1 * mockCharacteristic.setValue([0x2, 0x2] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), [] as byte[]))
            true
        }

        then:
        1 * mockCharacteristic.setValue([0x3, 0x3] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> false

        when:
        retryWriteOperationStrategy.triggerRetry()

        then:
        1 * mockCharacteristic.setValue([0x3, 0x3] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), [] as byte[]))
            true
        }

        testSubscriber.assertValuesEquals([[0x1, 0x1, 0x2, 0x2, 0x3, 0x3]] as byte[][])
        testSubscriber.assertComplete()
    }

    def "attempt to rewrite the failed batch if the strategy has emitted the LongWriteFailure - last batch, uneven count"() {
        given:
        RetryWriteOperation retryWriteOperationStrategy = givenWillRetryWriteOperation()
        this.writeOperationRetryStrategy = retryWriteOperationStrategy
        this.writeOperationAckStrategy = new ImmediateSerializedBatchAckStrategy()
        prepareObjectUnderTest(2, [0x1, 0x1, 0x2, 0x2, 0x3] as byte[])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockCharacteristic.setValue([0x1, 0x1] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), [] as byte[]))
            true
        }

        then:
        1 * mockCharacteristic.setValue([0x2, 0x2] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), [] as byte[]))
            true
        }

        then:
        1 * mockCharacteristic.setValue([0x3] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> false

        when:
        retryWriteOperationStrategy.triggerRetry()

        then:
        1 * mockCharacteristic.setValue([0x3] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), [] as byte[]))
            true
        }

        testSubscriber.assertValuesEquals([[0x1, 0x1, 0x2, 0x2, 0x3]] as byte[][])
        testSubscriber.assertComplete()
    }

    def "should release QueueReleaseInterface after successful write"() {

        given:
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        givenEachCharacteristicWriteOkAfterDefaultDelay()
        prepareObjectUnderTest(20, byteArray(60))

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()
        advanceTimeForWritesToComplete(3)

        then:
        1 * mockQueueReleaseInterface.release()
    }

    @Unroll
    def "should release QueueReleaseInterface when write failed to start"() {

        given:
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        givenCharacteristicWriteOkButEventuallyFailsToStart(failingWriteIndex)
        prepareObjectUnderTest(20, byteArray(60))

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()
        advanceTimeForWrites(failingWriteIndex)

        then:
        1 * mockQueueReleaseInterface.release()

        where:
        failingWriteIndex << [0, 1, 2]
    }

    @Unroll
    def "should release QueueReleaseInterface when write failed"() {

        given:
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        givenCharacteristicWriteOkButEventuallyFailsToStart(failingWriteIndex)
        prepareObjectUnderTest(20, byteArray(60))

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()
        advanceTimeForWritesToComplete(failingWriteIndex)

        then:
        1 * mockQueueReleaseInterface.release()

        where:
        failingWriteIndex << [0, 1, 2]
    }

    @Unroll
    def "should timeout if RxBleGattCallback.onCharacteristicWrite() won't trigger in 30 seconds"() {

        given:
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        givenCharacteristicWriteOkButEventuallyStalls(failingWriteIndex)
        prepareObjectUnderTest(20, byteArray(60))
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        advanceTimeForWritesToComplete(failingWriteIndex)

        then:
        testSubscriber.assertNoErrors()

        when:
        timeoutScheduler.advanceTimeBy(30, TimeUnit.SECONDS)

        then:
        testSubscriber.assertError(BleGattCallbackTimeoutException)

        and:
        testSubscriber.assertError {
            ((BleGattCallbackTimeoutException) it).getBleGattOperationType() == BleGattOperationType.CHARACTERISTIC_LONG_WRITE
        }

        where:
        failingWriteIndex << [0, 1, 2]
    }

    @Unroll
    def "should emit error if batchSizeProvider will provide value not greater than zero"() {

        given:
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        givenEachCharacteristicWriteOkAfterDefaultDelay()
        prepareObjectUnderTest(maxBatchSize, byteArray(20))

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertError(IllegalArgumentException)

        where:
        maxBatchSize << [0, -1]
    }

    @Unroll
    def "should release queue after next batch if unsubscribed"() {

        given:
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        prepareObjectUnderTest(1, byteArray(20))

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> true

        when:
        testSubscriber.dispose()

        then:
        0 * mockQueueReleaseInterface.release()

        when:
        batchWriteCallback.call(onCharacteristicWriteSubject, mockCharacteristic)

        then:
        0 * mockGatt.writeCharacteristic(mockCharacteristic) >> true

        and:
        1 * mockQueueReleaseInterface.release()

        where:
        batchWriteCallback << [
                { PublishSubject<ByteAssociation<UUID>> onWriteSubject, BluetoothGattCharacteristic characteristic ->
                    onWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), new byte[0]))
                },
                { PublishSubject<ByteAssociation<UUID>> onWriteSubject, BluetoothGattCharacteristic characteristic ->
                    onWriteSubject.onError(testException)
                }
        ]
    }

    def "unsuccessful writeCharacteristic() should not pass error to RxJavaPlugins.onErrorHandler() if Observable was disposed"() {

        given:
        def capturedException = captureRxUnhandledExceptions()
        def testScheduler = new TestScheduler()
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        givenCharacteristicWriteOkButEventuallyFailsToStart(0)
        prepareObjectUnderTest(1, byteArray(20), testScheduler)
        objectUnderTest.run(mockQueueReleaseInterface).test().dispose()

        when:
        testScheduler.triggerActions()

        then:
        capturedException.get() == null

        cleanup:
        RxJavaPlugins.setErrorHandler(null)
    }

    ////////////////////// Testing repetition logic implementation

    def "should emit repeat until ByteBuffer is empty"() {

        given:
        PublishSubject completionSubject = PublishSubject.create()
        QueueReleasingEmitterWrapper mockQueueReleasingEmitterWrapper = Mock(QueueReleasingEmitterWrapper)
        mockQueueReleasingEmitterWrapper.isWrappedEmitterUnsubscribed() >> false
        Observable repetitionObservable = CharacteristicLongWriteOperation
                .bufferIsNotEmptyAndOperationHasBeenAcknowledgedAndNotUnsubscribed(
                        new ImmediateSerializedBatchAckStrategy(),
                        ByteBuffer.allocate(0),
                        mockQueueReleasingEmitterWrapper
                )
                .apply(completionSubject)
        TestObserver testObserver = repetitionObservable.test()

        when:
        completionSubject.onNext(new Object())

        then:
        testObserver.assertNoValues()
    }

    ////////////////////// Helpers

    private void givenWillWriteNextBatchImmediatelyAfterPrevious() {
        writeOperationAckStrategy = new ImmediateSerializedBatchAckStrategy()
        writeOperationRetryStrategy = new NoRetryStrategy()
    }

    private AcknowledgementTrigger givenWillTriggerWriteAcknowledgement() {
        def trigger = new AcknowledgementTrigger()
        this.writeOperationAckStrategy = trigger
        return trigger
    }

    private RetryWriteOperation givenWillRetryWriteOperation() {
        def retry = new RetryWriteOperation()
        this.writeOperationRetryStrategy = retry
        return retry
    }

    class AcknowledgementTrigger implements RxBleConnection.WriteOperationAckStrategy {

        private final PublishSubject<Object> triggerSubject = PublishSubject.create()

        void acknowledgeWrite() {
            triggerSubject.with {
                onNext(true)
                onComplete()
            }
        }

        @Override
        ObservableSource<Boolean> apply(@NonNull Observable<Boolean> writeAck) {
            writeAck.flatMap(new Function<Boolean, Observable<Boolean>>() {
                @Override
                Observable<Boolean> apply(Boolean o) {
                    return triggerSubject
                }
            })
        }
    }

    class RetryWriteOperation implements RxBleConnection.WriteOperationRetryStrategy {

        private final PublishSubject triggerSubject = PublishSubject.create()

        void triggerRetry() {
            triggerSubject.with {
                onNext(true)
                onComplete()
            }
        }

        @Override
        Observable<RxBleConnection.WriteOperationRetryStrategy.LongWriteFailure> apply(
                Observable<RxBleConnection.WriteOperationRetryStrategy.LongWriteFailure> longWriteFailureObservable) {
            return longWriteFailureObservable.flatMap({ longWriteFailure ->
                return triggerSubject.map({ aBoolean ->
                    return longWriteFailure
                })
            })
        }
    }

    private static byte[] byteArray(int size) {
        byte[] bytes = new byte[size]
        for (int i = 0; i < size; i++) {
            bytes[i] = i
        }
        return bytes
    }

    private static byte[] subSequence(byte[] bytes, int startIndex, int endIndexNotIncluded) {
        int size = endIndexNotIncluded - startIndex
        byte[] subSequence = new byte[size]
        ByteBuffer.wrap(bytes).position(startIndex).get(subSequence)
        return subSequence
    }

    private static byte[][] expectedBatches(byte[] bytes, int maxBatchSize) {
        int writtenBytesLength = bytes.length
        boolean excess = writtenBytesLength % maxBatchSize > 0
        int expectedBatchesLength = writtenBytesLength / maxBatchSize + (excess ? 1 : 0)
        byte[][] expectedBatches = new byte[expectedBatchesLength][]
        for (int i = 0; i < expectedBatchesLength; i++) {
            int startIndex = i * maxBatchSize
            int endIndex = (i + 1) * maxBatchSize
            endIndex = Math.min(endIndex, writtenBytesLength)
            expectedBatches[i] = subSequence(bytes, startIndex, endIndex)
        }
        return expectedBatches
    }

    private advanceTimeForWrites(long numberOfWrites) {
        testScheduler.advanceTimeBy(numberOfWrites * DEFAULT_WRITE_DELAY, TimeUnit.SECONDS)
    }

    private advanceTimeForWritesToComplete(long numberOfWrites) {
        advanceTimeForWrites(numberOfWrites + 1)
    }

    private givenEachCharacteristicWriteOkAfterDefaultDelay() {
        mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            UUID uuid = characteristic.getUuid()
            byte[] returnBytes = new byte[0]
            testScheduler.createWorker().schedule({
                onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(uuid, returnBytes))
            }, DEFAULT_WRITE_DELAY, TimeUnit.SECONDS)

            true
        }
    }

    private givenCharacteristicWriteStartsOkButDifferentCharacteristicOnCallbackFirst() {
        mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            byte[] returnBytes = new byte[0]
            def worker = testScheduler.createWorker()
            worker.schedule({
                onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(differentCharacteristicUUID, returnBytes))
            }, DEFAULT_WRITE_DELAY, TimeUnit.SECONDS)
            worker.schedule({
                onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), returnBytes))
            }, DEFAULT_WRITE_DELAY * 2, TimeUnit.SECONDS)

            true
        }
    }

    private givenCharacteristicWriteOkButEventuallyFailsToStart(int failingWriteIndex) {
        AtomicInteger writeIndex = new AtomicInteger(0)

        mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            UUID uuid = characteristic.getUuid()
            byte[] returnBytes = new byte[0]
            int currentIndex = writeIndex.getAndIncrement()
            if (currentIndex == failingWriteIndex) {
                return false
            }

            testScheduler.createWorker().schedule({
                onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(uuid, returnBytes))
            }, DEFAULT_WRITE_DELAY, TimeUnit.SECONDS)

            true
        }
    }

    private givenCharacteristicWriteOkButEventuallyFails(int failingWriteIndex) {
        AtomicInteger writeIndex = new AtomicInteger(0)

        mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            UUID uuid = characteristic.getUuid()
            byte[] returnBytes = new byte[0]

            testScheduler.createWorker().schedule({
                int currentIndex = writeIndex.getAndIncrement()
                if (currentIndex == failingWriteIndex) {
                    onCharacteristicWriteSubject.onError(testException)
                } else {
                    onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(uuid, returnBytes))
                }
            }, DEFAULT_WRITE_DELAY, TimeUnit.SECONDS)

            true
        }
    }

    private givenCharacteristicWriteOkButEventuallyStalls(int failingWriteIndex) {
        AtomicInteger writeIndex = new AtomicInteger(0)

        mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            UUID uuid = characteristic.getUuid()
            byte[] returnBytes = new byte[0]

            testScheduler.createWorker().schedule({
                int currentIndex = writeIndex.getAndIncrement()
                if (currentIndex != failingWriteIndex) {
                    onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(uuid, returnBytes))
                }
            }, DEFAULT_WRITE_DELAY, TimeUnit.SECONDS)

            true
        }
    }

    private static AtomicReference<Throwable> captureRxUnhandledExceptions() {
        AtomicReference<Throwable> unhandledExceptionAtomicReference = new AtomicReference<>()
        RxJavaPlugins.setErrorHandler({ throwable ->
            unhandledExceptionAtomicReference.set(throwable)
        })
        return unhandledExceptionAtomicReference
    }

    private prepareObjectUnderTest(int maxBatchSize, byte[] testData) {
        prepareObjectUnderTest(maxBatchSize, testData, immediateScheduler)
    }

    private prepareObjectUnderTest(int maxBatchSize, byte[] testData, Scheduler scheduler) {
        objectUnderTest = new CharacteristicLongWriteOperation(
                mockGatt,
                mockCallback,
                scheduler,
                new MockOperationTimeoutConfiguration(10, timeoutScheduler),
                mockCharacteristic,
                { maxBatchSize },
                writeOperationAckStrategy,
                writeOperationRetryStrategy,
                testData
        )
    }
}