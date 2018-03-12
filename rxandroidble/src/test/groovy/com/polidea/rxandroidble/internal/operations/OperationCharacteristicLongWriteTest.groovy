package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble.exceptions.BleGattCharacteristicException
import com.polidea.rxandroidble.exceptions.BleGattOperationType
import com.polidea.rxandroidble.internal.connection.ImmediateSerializedBatchAckStrategy
import com.polidea.rxandroidble.internal.connection.NoRetryStrategy
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble.internal.util.ByteAssociation
import com.polidea.rxandroidble.internal.util.MockOperationTimeoutConfiguration
import rx.Observable
import rx.functions.Func1
import rx.internal.schedulers.ImmediateScheduler
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import rx.subjects.PublishSubject
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

public class OperationCharacteristicLongWriteTest extends Specification {

    private static long DEFAULT_WRITE_DELAY = 1
    UUID mockCharacteristicUUID = UUID.randomUUID()
    UUID differentCharacteristicUUID = UUID.randomUUID()
    BluetoothGatt mockGatt = Mock BluetoothGatt
    BluetoothDevice mockDevice = Mock BluetoothDevice
    RxBleGattCallback mockCallback = Mock RxBleGattCallback
    BluetoothGattCharacteristic mockCharacteristic = Mock BluetoothGattCharacteristic
    RxBleConnection.WriteOperationAckStrategy writeOperationAckStrategy
    RxBleConnection.WriteOperationRetryStrategy writeOperationRetryStrategy
    def testSubscriber = new TestSubscriber()
    TestScheduler testScheduler = new TestScheduler()
    TestScheduler timeoutScheduler = new TestScheduler()
    ImmediateScheduler immediateScheduler = ImmediateScheduler.INSTANCE
    PublishSubject<ByteAssociation<UUID>> onCharacteristicWriteSubject = PublishSubject.create()
    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface
    CharacteristicLongWriteOperation objectUnderTest
    @Shared
    Exception testException = new Exception("testException")
    BleGattCharacteristicException bleGattCharacteristicException = Mock BleGattCharacteristicException

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
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)

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
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)
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
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)
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
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)
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
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)
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

        when:
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)

        then:
        (1.._) * mockCallback.getOnCharacteristicWrite() >> Observable.empty()
    }

    def "should complete part of the write if RxBleGattCallback.onCharacteristicWrite() will be called with proper characteristic"() {

        given:
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        givenCharacteristicWriteStartsOkButDifferentCharacteristicOnCallbackFirst()
        prepareObjectUnderTest(20, byteArray(60))

        when:
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)
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
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)
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
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)

        then:
        1 * mockCharacteristic.setValue([0x1, 0x1] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> false
        testSubscriber.assertError(BleGattCannotStartException)
    }

    def "attempt to rewrite the failed batch if the strategy has emitted the LongWriteFailure - first batch"() {
        given:
        this.writeOperationRetryStrategy = givenWillRetryWriteOperation()
        this.writeOperationAckStrategy = new ImmediateSerializedBatchAckStrategy()
        prepareObjectUnderTest(2, [0x1, 0x1, 0x2, 0x2, 0x3, 0x3] as byte[])

        when:
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)

        then:
        1 * mockCharacteristic.setValue([0x1, 0x1] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> false
        testSubscriber.assertNoTerminalEvent()

        when:
        writeOperationRetryStrategy.triggerRetry()

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

        testSubscriber.assertValueEquals([0x1, 0x1, 0x2, 0x2, 0x3, 0x3] as byte[])
        testSubscriber.assertCompleted()
    }

    def "attempt to rewrite the failed batch if the strategy has emitted the LongWriteFailure - mid batch"() {
        given:
        this.writeOperationRetryStrategy = givenWillRetryWriteOperation()
        this.writeOperationAckStrategy = new ImmediateSerializedBatchAckStrategy()
        prepareObjectUnderTest(2, [0x1, 0x1, 0x2, 0x2, 0x3, 0x3] as byte[])

        when:
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)

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
        writeOperationRetryStrategy.triggerRetry()

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

        testSubscriber.assertValueEquals([0x1, 0x1, 0x2, 0x2, 0x3, 0x3] as byte[])
        testSubscriber.assertCompleted()
    }

    def "attempt to rewrite the failed batch if the strategy has emitted the LongWriteFailure - last batch"() {
        given:
        this.writeOperationRetryStrategy = givenWillRetryWriteOperation()
        this.writeOperationAckStrategy = new ImmediateSerializedBatchAckStrategy()
        prepareObjectUnderTest(2, [0x1, 0x1, 0x2, 0x2, 0x3, 0x3] as byte[])

        when:
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)

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
        writeOperationRetryStrategy.triggerRetry()

        then:
        1 * mockCharacteristic.setValue([0x3, 0x3] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), [] as byte[]))
            true
        }

        testSubscriber.assertValueEquals([0x1, 0x1, 0x2, 0x2, 0x3, 0x3] as byte[])
        testSubscriber.assertCompleted()
    }

    def "attempt to rewrite the failed batch if the strategy has emitted the LongWriteFailure - last batch, uneven count"() {
        given:
        this.writeOperationRetryStrategy = givenWillRetryWriteOperation()
        this.writeOperationAckStrategy = new ImmediateSerializedBatchAckStrategy()
        prepareObjectUnderTest(2, [0x1, 0x1, 0x2, 0x2, 0x3] as byte[])

        when:
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)

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
        writeOperationRetryStrategy.triggerRetry()

        then:
        1 * mockCharacteristic.setValue([0x3] as byte[]) >> true
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            onCharacteristicWriteSubject.onNext(new ByteAssociation<UUID>(characteristic.getUuid(), [] as byte[]))
            true
        }

        testSubscriber.assertValueEquals([0x1, 0x1, 0x2, 0x2, 0x3] as byte[])
        testSubscriber.assertCompleted()
    }

    def "should release QueueReleaseInterface after successful write"() {

        given:
        givenWillWriteNextBatchImmediatelyAfterPrevious()
        givenEachCharacteristicWriteOkAfterDefaultDelay()
        prepareObjectUnderTest(20, byteArray(60))

        when:
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)
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
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)
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
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)
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
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)

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
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)

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
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)

        then:
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> true

        when:
        testSubscriber.unsubscribe()

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

        public void acknowledgeWrite() {
            triggerSubject.with {
                onNext(true)
                onCompleted()
            }
        }

        @Override
        Observable<Boolean> call(Observable<Boolean> writeAck) {
            return writeAck.flatMap(new Func1<Boolean, Observable<Boolean>>() {
                @Override
                Observable<Boolean> call(Boolean o) {
                    return triggerSubject
                }
            })
        }
    }

    class RetryWriteOperation implements RxBleConnection.WriteOperationRetryStrategy {

        private final PublishSubject triggerSubject = PublishSubject.create()

        public void triggerRetry() {
            triggerSubject.with {
                onNext(true)
                onCompleted()
            }
        }

        @Override
        Observable<RxBleConnection.WriteOperationRetryStrategy.LongWriteFailure> call(
                Observable<RxBleConnection.WriteOperationRetryStrategy.LongWriteFailure> longWriteFailureObservable) {
            return longWriteFailureObservable.flatMap({ longWriteFailure ->
                return triggerSubject.map({ aBoolean ->
                    return longWriteFailure
                })
            })
        }
    }

    private static byte[] byteArray(int size) {
        byte[] bytes = new byte[size];
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

    private givenCharacteristicWriteOkButEventuallyFailsToWrite(int failingWriteIndex) {
        AtomicInteger writeIndex = new AtomicInteger(0)

        mockGatt.writeCharacteristic(mockCharacteristic) >> { BluetoothGattCharacteristic characteristic ->
            UUID uuid = characteristic.getUuid()
            byte[] returnBytes = new byte[0]

            testScheduler.createWorker().schedule({
                int currentIndex = writeIndex.getAndIncrement()
                if (currentIndex == failingWriteIndex) {
                    onCharacteristicWriteSubject.onError(bleGattCharacteristicException)
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

    private prepareObjectUnderTest(int maxBatchSize, byte[] testData) {
        objectUnderTest = new CharacteristicLongWriteOperation(
                mockGatt,
                mockCallback,
                immediateScheduler,
                new MockOperationTimeoutConfiguration(10, timeoutScheduler),
                mockCharacteristic,
                { maxBatchSize },
                writeOperationAckStrategy,
                writeOperationRetryStrategy,
                testData
        )
    }
}