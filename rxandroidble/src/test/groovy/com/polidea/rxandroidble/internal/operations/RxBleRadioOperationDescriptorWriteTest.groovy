package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble.exceptions.BleGattOperationType
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble.internal.util.ByteAssociation
import java.util.concurrent.Semaphore
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject
import spock.lang.Specification

public class RxBleRadioOperationDescriptorWriteTest extends Specification {

    BluetoothGatt mockGatt = Mock BluetoothGatt

    RxBleGattCallback mockCallback = Mock RxBleGattCallback

    BluetoothGattDescriptor mockDescriptor = Mock BluetoothGattDescriptor

    BluetoothGattDescriptor differentDescriptor = Mock BluetoothGattDescriptor

    def testSubscriber = new TestSubscriber()

    PublishSubject<ByteAssociation<BluetoothGattDescriptor>> onDescriptorWriteSubject = PublishSubject.create()

    Semaphore mockSemaphore = Mock Semaphore

    RxBleRadioOperationDescriptorWrite objectUnderTest

    byte[] testData = ['t', 'e', 's', 't']

    def setup() {
        mockCallback.getOnDescriptorWrite() >> onDescriptorWriteSubject
        prepareObjectUnderTest()
    }

    def "should call only once BluetoothGattDescriptor.setValue() before calling BluetoothGatt.writeDescriptor() on single write when run()"() {

        when:
        objectUnderTest.run()

        then:
        1 * mockDescriptor.setValue(testData) >> true

        then:
        1 * mockGatt.writeDescriptor(mockDescriptor) >> true
    }

    def "asObservable() should not emit error when BluetoothGatt.writeDescriptor() returns true while run()"() {

        given:
        givenDescriptorWithUUIDWritesData([descriptor: mockDescriptor, value: []])

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit error when BluetoothGatt.writeDescriptor() returns false while run()"() {

        given:
        givenDescriptorWriteFailToStart()

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertError BleGattCannotStartException

        and:
        testSubscriber.assertError {
            it.getBleGattOperationType() == BleGattOperationType.DESCRIPTOR_WRITE
        }
    }

    def "asObservable() should emit error when RxBleGattCallback.getOnDescriptorWrite() emits error"() {

        given:
        Throwable testException = new Throwable("test")
        shouldEmitErrorOnDescriptorWrite(testException)

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertError testException
    }

    def "asObservable() should not emit when RxBleGattCallback.getOnDescriptorWrite() emits before run()"() {

        given:
        mockGatt.writeDescriptor(mockDescriptor) >> true
        onDescriptorWriteSubject.onNext(new ByteAssociation<BluetoothGattDescriptor>(mockDescriptor, new byte[0]))

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertNoValues()

        and:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit next when RxBleGattCallback.getOnDescriptorWrite() emits next"() {

        given:
        byte[] dataFromCharacteristic = []
        givenDescriptorWithUUIDWritesData([descriptor: mockDescriptor, value: dataFromCharacteristic])
        prepareObjectUnderTest()

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertValue dataFromCharacteristic
    }

    def "asObservable() emit only first descriptor value notified"() {

        given:
        byte[] dataFromCharacteristic = [3, 4, 5]
        byte[] secondValueFromCharacteristic = [1, 2, 3]
        givenDescriptorWithUUIDWritesData(
                [descriptor: mockDescriptor, value: dataFromCharacteristic],
                [descriptor: mockDescriptor, value: secondValueFromCharacteristic])
        prepareObjectUnderTest()

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertValueCount 1

        and:
        testSubscriber.assertValue dataFromCharacteristic
    }

    def "asObservable() not emit descriptor value if BLE notified only with non matching descriptor"() {

        given:
        givenDescriptorWithUUIDWritesData([descriptor: differentDescriptor, value: []])

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertValueCount 0
    }

    def "asObservable() emit only value of matching descriptor"() {

        given:
        byte[] dataFromCharacteristic = [3, 4, 5]
        byte[] secondValueFromCharacteristic = [1, 2, 3]
        givenDescriptorWithUUIDWritesData(
                [descriptor: differentDescriptor, value: dataFromCharacteristic],
                [descriptor: mockDescriptor, value: secondValueFromCharacteristic])
        prepareObjectUnderTest()

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertValueCount 1

        and:
        testSubscriber.assertValue secondValueFromCharacteristic
    }

    def "should release Semaphore after successful write"() {

        given:
        givenDescriptorWithUUIDWritesData([descriptor: mockDescriptor, value: []])

        when:
        objectUnderTest.run()

        then:
        1 * mockSemaphore.release()
    }

    def "should release Semaphore when write failed to start"() {

        given:
        givenDescriptorWriteFailToStart()

        when:
        objectUnderTest.run()

        then:
        1 * mockSemaphore.release()
    }

    def "should release Semaphore when write failed"() {
        given:
        shouldEmitErrorOnDescriptorWrite(new Throwable("test"))

        when:
        objectUnderTest.run()

        then:
        1 * mockSemaphore.release()
    }

    private givenDescriptorWithUUIDWritesData(Map... returnedDataOnWrite) {
        mockGatt.writeDescriptor(mockDescriptor) >> {
            returnedDataOnWrite.each {
                onDescriptorWriteSubject.onNext(ByteAssociation.create(it['descriptor'] as BluetoothGattDescriptor, it['value'] as byte[]))
            }

            true
        }
    }

    private shouldEmitErrorOnDescriptorWrite(Throwable testException) {
        mockGatt.writeDescriptor(mockDescriptor) >> {
            onDescriptorWriteSubject.onError(testException)
            true
        }
    }

    private givenDescriptorWriteFailToStart() {
        mockGatt.writeDescriptor(mockDescriptor) >> false
    }

    private prepareObjectUnderTest() {
        objectUnderTest = new RxBleRadioOperationDescriptorWrite(mockCallback, mockGatt, mockDescriptor, testData)
        objectUnderTest.setRadioBlockingSemaphore(mockSemaphore)
        objectUnderTest.asObservable().subscribe(testSubscriber)
    }
}