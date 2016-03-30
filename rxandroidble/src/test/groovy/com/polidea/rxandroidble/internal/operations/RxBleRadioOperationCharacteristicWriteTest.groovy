package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.support.v4.util.Pair
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble.exceptions.BleGattOperationType
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject
import spock.lang.Specification

import java.util.concurrent.Semaphore

public class RxBleRadioOperationCharacteristicWriteTest extends Specification {

    UUID mockCharacteristicUUID = UUID.randomUUID()
    UUID differentCharacteristicUUID = UUID.randomUUID()
    BluetoothGatt mockGatt = Mock BluetoothGatt
    RxBleGattCallback mockCallback = Mock RxBleGattCallback
    BluetoothGattCharacteristic mockCharacteristic = Mock BluetoothGattCharacteristic
    def testSubscriber = new TestSubscriber()
    PublishSubject<Pair<UUID, byte[]>> onCharacteristicWriteSubject = PublishSubject.create()
    Semaphore mockSemaphore = Mock Semaphore
    RxBleRadioOperationCharacteristicWrite objectUnderTest

    def setup() {
        mockCharacteristic.getUuid() >> mockCharacteristicUUID
        mockCallback.getOnCharacteristicWrite() >> onCharacteristicWriteSubject
        prepareObjectUnderTest()
    }

    def "should call BluetoothGatt.writeCharacteristic() only once on single write when run()"() {

        when:
        objectUnderTest.run()

        then:
        1 * mockGatt.writeCharacteristic(mockCharacteristic) >> true
    }

    def "asObservable() should not emit error when BluetoothGatt.writeCharacteristic() returns true while run()"() {

        given:
        givenCharacteristicWithUUIDWritesData([uuid: mockCharacteristicUUID, value: []])

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit error when BluetoothGatt.writeCharacteristic() returns false while run()"() {

        given:
        givenCharacteristicWriteFailToStart()

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertError BleGattCannotStartException

        and:
        testSubscriber.assertError {
            it.getBleGattOperationType() == BleGattOperationType.CHARACTERISTIC_WRITE
        }
    }

    def "asObservable() should emit error when RxBleGattCallback.getOnCharacteristicWrite() emits error"() {

        given:
        Throwable testException = new Throwable("test")
        shouldEmitErrorOnCharacteristicWrite(testException)

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertError testException
    }

    def "asObservable() should not emit when RxBleGattCallback.getOnCharacteristicWrite() emits before run()"() {

        given:
        mockGatt.writeCharacteristic(mockCharacteristic) >> true
        onCharacteristicWriteSubject.onNext(new Pair(mockCharacteristicUUID, []))

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertNoValues()

        and:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit next when RxBleGattCallback.getOnCharacteristicWrite() emits next"() {

        given:
        byte[] dataFromCharacteristic = []
        givenCharacteristicWithUUIDWritesData([uuid: mockCharacteristicUUID, value: dataFromCharacteristic])
        prepareObjectUnderTest()

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertValue mockCharacteristic
    }

    def "asObservable() emit only first characteristic value notified"() {

        given:
        byte[] dataFromCharacteristic = [3, 4, 5]
        byte[] secondValueFromCharacteristic = [1, 2, 3]
        givenCharacteristicWithUUIDWritesData(
                [uuid: mockCharacteristicUUID, value: dataFromCharacteristic],
                [uuid: mockCharacteristicUUID, value: secondValueFromCharacteristic])
        prepareObjectUnderTest()

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertValueCount 1

        and:
        testSubscriber.assertValue mockCharacteristic
    }

    def "asObservable() not emit characteristic value if BLE notified only with non matching characteristics"() {

        given:
        givenCharacteristicWithUUIDWritesData([uuid: differentCharacteristicUUID, value: []])

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertValueCount 0
    }

    def "asObservable() emit only characteristic value with matching UUID"() {

        given:
        byte[] dataFromCharacteristic = [3, 4, 5]
        byte[] secondValueFromCharacteristic = [1, 2, 3]
        givenCharacteristicWithUUIDWritesData(
                [uuid: differentCharacteristicUUID, value: dataFromCharacteristic],
                [uuid: mockCharacteristicUUID, value: secondValueFromCharacteristic])
        prepareObjectUnderTest()

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertValueCount 1

        and:
        testSubscriber.assertValue mockCharacteristic
    }

    def "should release Semaphore after successful write"() {

        given:
        givenCharacteristicWithUUIDWritesData([uuid: mockCharacteristicUUID, value: []])

        when:
        objectUnderTest.run()

        then:
        1 * mockSemaphore.release()
    }

    def "should release Semaphore when write failed to start"() {

        given:
        givenCharacteristicWriteFailToStart()

        when:
        objectUnderTest.run()

        then:
        1 * mockSemaphore.release()
    }

    def "should release Semaphore when write failed"() {
        given:
        shouldEmitErrorOnCharacteristicWrite(new Throwable("test"))

        when:
        objectUnderTest.run()

        then:
        1 * mockSemaphore.release()
    }

    private givenCharacteristicWithUUIDWritesData(Map... returnedDataOnWrite) {
        mockGatt.writeCharacteristic(mockCharacteristic) >> {
            returnedDataOnWrite.each {
                onCharacteristicWriteSubject.onNext(new Pair(it['uuid'], it['value'] as byte[]))
            }

            true
        }
    }

    private shouldEmitErrorOnCharacteristicWrite(Throwable testException) {
        mockGatt.writeCharacteristic(mockCharacteristic) >> {
            onCharacteristicWriteSubject.onError(testException)
            true
        }
    }

    private givenCharacteristicWriteFailToStart() {
        mockGatt.writeCharacteristic(mockCharacteristic) >> false
    }

    private prepareObjectUnderTest() {
        objectUnderTest = new RxBleRadioOperationCharacteristicWrite(mockCallback, mockGatt, mockCharacteristic)
        objectUnderTest.setRadioBlockingSemaphore(mockSemaphore)
        objectUnderTest.asObservable().subscribe(testSubscriber)
    }
}