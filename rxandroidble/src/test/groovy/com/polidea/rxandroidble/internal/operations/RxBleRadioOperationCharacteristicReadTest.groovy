package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble.exceptions.BleGattOperationType
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble.internal.util.ByteAssociation
import com.polidea.rxandroidble.internal.util.MockOperationTimeoutConfiguration

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import rx.subjects.PublishSubject
import spock.lang.Specification

public class RxBleRadioOperationCharacteristicReadTest extends Specification {

    UUID mockCharacteristicUUID = UUID.randomUUID()
    UUID differentCharacteristicUUID = UUID.randomUUID()
    BluetoothGatt mockGatt = Mock BluetoothGatt
    RxBleGattCallback mockCallback = Mock RxBleGattCallback
    BluetoothGattCharacteristic mockCharacteristic = Mock BluetoothGattCharacteristic
    TestSubscriber<byte[]> testSubscriber = new TestSubscriber()
    PublishSubject<ByteAssociation<UUID>> onCharacteristicReadSubject = PublishSubject.create()
    Semaphore mockSemaphore = Mock Semaphore
    TestScheduler testScheduler = new TestScheduler()
    RxBleRadioOperationCharacteristicRead objectUnderTest

    def setup() {
        objectUnderTest = new RxBleRadioOperationCharacteristicRead(mockCallback, mockGatt,
                new MockOperationTimeoutConfiguration(testScheduler), mockCharacteristic)
        mockCharacteristic.getUuid() >> mockCharacteristicUUID
        mockCallback.getOnCharacteristicRead() >> onCharacteristicReadSubject
        objectUnderTest.setRadioBlockingSemaphore(mockSemaphore)
        objectUnderTest.asObservable().subscribe(testSubscriber)
    }

    def "should call BluetoothGatt.readCharacteristic() only once on single read when run()"() {

        when:
        objectUnderTest.run()

        then:
        1 * mockGatt.readCharacteristic(mockCharacteristic) >> true
    }

    def "asObservable() should not emit error when BluetoothGatt.readCharacteristic() returns true while run()"() {

        given:
        givenCharacteristicWithUUIDContainData([uuid: mockCharacteristicUUID, value: []])

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit error when BluetoothGatt.readCharacteristic() returns false while run()"() {

        given:
        givenCharacteristicReadFailToStart()

        when:
        objectUnderTest.run()

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
        objectUnderTest.run()

        then:
        testSubscriber.assertError testException
    }

    def "asObservable() should not emit when RxBleGattCallback.getOnCharacteristicRead() emits before run()"() {
        // XXX [PU] I'm not sure if it is really desired
        given:
        mockGatt.readCharacteristic(mockCharacteristic) >> true
        onCharacteristicReadSubject.onNext(new ByteAssociation(mockCharacteristicUUID, new byte[0]))

        when:
        objectUnderTest.run()

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
        objectUnderTest.run()

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
        objectUnderTest.run()

        then:
        testSubscriber.assertValueCount 1

        and:
        testSubscriber.assertValue dataFromCharacteristic
    }

    def "asObservable() not emit characteristic value if BLE notified only with non matching characteristics"() {

        given:
        givenCharacteristicWithUUIDContainData([uuid: differentCharacteristicUUID, value: []])

        when:
        objectUnderTest.run()

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
        objectUnderTest.run()

        then:
        testSubscriber.assertValueCount 1

        and:
        testSubscriber.assertValue secondValueFromCharacteristic
    }

    def "should release Semaphore after successful read"() {

        given:
        givenCharacteristicWithUUIDContainData([uuid: mockCharacteristicUUID, value: []])

        when:
        objectUnderTest.run()

        then:
        1 * mockSemaphore.release()
    }

    def "should release Semaphore when read failed to start"() {

        given:
        givenCharacteristicReadFailToStart()

        when:
        objectUnderTest.run()

        then:
        1 * mockSemaphore.release()
    }

    def "should release Semaphore when read failed"() {
        given:
        shouldEmitErrorOnCharacteristicRead(new Throwable("test"))

        when:
        objectUnderTest.run()

        then:
        1 * mockSemaphore.release()
    }

    def "should timeout if RxBleGattCallback.onCharacteristicRead() won't trigger in 30 seconds"() {

        given:
        givenCharacteristicReadStartsOk()
        objectUnderTest.run()

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