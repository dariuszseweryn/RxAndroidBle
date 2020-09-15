package com.polidea.rxandroidble2.internal.operations

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.polidea.rxandroidble2.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble2.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble2.exceptions.BleGattOperationType
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble2.internal.util.ByteAssociation
import com.polidea.rxandroidble2.internal.util.MockOperationTimeoutConfiguration
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import spock.lang.Specification

import java.util.concurrent.TimeUnit

public class OperationCharacteristicReadTest extends Specification {

    UUID mockCharacteristicUUID = UUID.randomUUID()
    UUID differentCharacteristicUUID = UUID.randomUUID()
    BluetoothGatt mockGatt = Mock BluetoothGatt
    RxBleGattCallback mockCallback = Mock RxBleGattCallback
    BluetoothGattCharacteristic mockCharacteristic = Mock BluetoothGattCharacteristic
    PublishSubject<ByteAssociation<UUID>> onCharacteristicReadSubject = PublishSubject.create()
    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface
    TestScheduler testScheduler = new TestScheduler()
    CharacteristicReadOperation objectUnderTest

    def setup() {
        objectUnderTest = new CharacteristicReadOperation(mockCallback, mockGatt,
                new MockOperationTimeoutConfiguration(testScheduler), mockCharacteristic)
        mockCharacteristic.getUuid() >> mockCharacteristicUUID
        mockCallback.getOnCharacteristicRead() >> onCharacteristicReadSubject
    }

    def "should call BluetoothGatt.readCharacteristic() only once on single read when run()"() {

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockGatt.readCharacteristic(mockCharacteristic) >> true
    }

    def "asObservable() should not emit error when BluetoothGatt.readCharacteristic() returns true while run()"() {

        given:
        givenCharacteristicWithUUIDContainData([uuid: mockCharacteristicUUID, value: []])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit error when BluetoothGatt.readCharacteristic() returns false while run()"() {

        given:
        givenCharacteristicReadFailToStart()

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertError BleGattCannotStartException

        and:
        testSubscriber.assertError {
            it.getBleGattOperationType() == BleGattOperationType.CHARACTERISTIC_READ
        }
    }

    def "asObservable() should emit error when RxBleGattCallback.getOnCharacteristicRead() emits error"() {

        given:
        Throwable testException = new Throwable("test")
        shouldEmitErrorOnCharacteristicRead(testException)

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertError testException
    }

    def "asObservable() should not emit when RxBleGattCallback.getOnCharacteristicRead() emits before run()"() {
        // XXX [PU] I'm not sure if it is really desired
        given:
        mockGatt.readCharacteristic(mockCharacteristic) >> true
        onCharacteristicReadSubject.onNext(new ByteAssociation(mockCharacteristicUUID, new byte[0]))

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertNoValues()

        and:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit next when RxBleGattCallback.getOnCharacteristicRead() emits next"() {

        given:
        byte[] dataFromCharacteristic = []
        givenCharacteristicWithUUIDContainData([uuid: mockCharacteristicUUID, value: dataFromCharacteristic])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertValue dataFromCharacteristic
    }

    def "asObservable() emit only first characteristic value notified"() {

        given:
        byte[] dataFromCharacteristic = [3, 4, 5]
        byte[] secondValueFromCharacteristic = [1, 2, 3]
        givenCharacteristicWithUUIDContainData(
                [uuid: mockCharacteristicUUID, value: dataFromCharacteristic],
                [uuid: mockCharacteristicUUID, value: secondValueFromCharacteristic])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertValueCount 1

        and:
        testSubscriber.assertValue dataFromCharacteristic
    }

    def "asObservable() not emit characteristic value if BLE notified only with non matching characteristics"() {

        given:
        givenCharacteristicWithUUIDContainData([uuid: differentCharacteristicUUID, value: []])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertValueCount 0
    }

    def "asObservable() emit only characteristic value with matching UUID"() {

        given:
        byte[] dataFromCharacteristic = [3, 4, 5]
        byte[] secondValueFromCharacteristic = [1, 2, 3]
        givenCharacteristicWithUUIDContainData(
                [uuid: differentCharacteristicUUID, value: dataFromCharacteristic],
                [uuid: mockCharacteristicUUID, value: secondValueFromCharacteristic])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertValueCount 1

        and:
        testSubscriber.assertValue secondValueFromCharacteristic
    }

    def "should release QueueReleaseInterface after successful read"() {

        given:
        givenCharacteristicWithUUIDContainData([uuid: mockCharacteristicUUID, value: []])

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockQueueReleaseInterface.release()
    }

    def "should release QueueReleaseInterface when read failed to start"() {

        given:
        givenCharacteristicReadFailToStart()

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockQueueReleaseInterface.release()
    }

    def "should release QueueReleaseInterface when read failed"() {
        given:
        shouldEmitErrorOnCharacteristicRead(new Throwable("test"))

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockQueueReleaseInterface.release()
    }

    def "should timeout if RxBleGattCallback.onCharacteristicRead() won't trigger in 30 seconds"() {

        given:
        givenCharacteristicReadStartsOk()
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)

        then:
        testSubscriber.assertError(BleGattCallbackTimeoutException)

        and:
        testSubscriber.assertError {
            ((BleGattCallbackTimeoutException)it).getBleGattOperationType() == BleGattOperationType.CHARACTERISTIC_READ
        }
    }

    private givenCharacteristicWithUUIDContainData(Map... returnedDataOnRead) {
        mockGatt.readCharacteristic(mockCharacteristic) >> {
            returnedDataOnRead.each {
                onCharacteristicReadSubject.onNext(new ByteAssociation(it['uuid'], it['value'] as byte[]))
            }

            true
        }
    }

    private shouldEmitErrorOnCharacteristicRead(Throwable testException) {
        mockGatt.readCharacteristic(mockCharacteristic) >> {
            onCharacteristicReadSubject.onError(testException)
            true
        }
    }

    private givenCharacteristicReadFailToStart() {
        mockGatt.readCharacteristic(mockCharacteristic) >> false
    }

    private givenCharacteristicReadStartsOk() {
        mockGatt.readCharacteristic(mockCharacteristic) >> true
    }
}