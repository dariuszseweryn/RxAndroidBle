package com.polidea.rxandroidble2.internal.operations

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
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
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

public class OperationDescriptorWriteTest extends Specification {

    BluetoothGatt mockGatt = Mock BluetoothGatt
    RxBleGattCallback mockCallback = Mock RxBleGattCallback
    BluetoothGattDescriptor mockDescriptor = Mock BluetoothGattDescriptor
    BluetoothGattDescriptor differentDescriptor = Mock BluetoothGattDescriptor
    BluetoothGattCharacteristic mockParentCharacteristic = Mock BluetoothGattCharacteristic
    TestScheduler testScheduler = new TestScheduler()
    PublishSubject<ByteAssociation<BluetoothGattDescriptor>> onDescriptorWriteSubject = PublishSubject.create()
    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface
    DescriptorWriteOperation objectUnderTest
    byte[] testData = ['t', 'e', 's', 't']
    int bluetoothGattCharacteristicDefaultWriteType = 99
    int originalParentBluetoothGattCharacteristicWriteType = 1337

    def setup() {
        mockCallback.getOnDescriptorWrite() >> onDescriptorWriteSubject
        mockDescriptor.getCharacteristic() >> mockParentCharacteristic
        mockParentCharacteristic.getWriteType() >> originalParentBluetoothGattCharacteristicWriteType
        prepareObjectUnderTest()
    }

    def "should call only once BluetoothGattDescriptor.setValue() before calling BluetoothGatt.writeDescriptor() on single write when run()"() {

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockDescriptor.setValue(testData) >> true

        then:
        1 * mockGatt.writeDescriptor(mockDescriptor) >> true
    }

    def "asObservable() should not emit error when BluetoothGatt.writeDescriptor() returns true while run()"() {

        given:
        givenDescriptorWithUUIDWritesData([descriptor: mockDescriptor, value: []])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit error when BluetoothGatt.writeDescriptor() returns false while run()"() {

        given:
        givenDescriptorWriteFailToStart()

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

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
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertError testException
    }

    def "asObservable() should not emit when RxBleGattCallback.getOnDescriptorWrite() emits before run()"() {

        given:
        mockGatt.writeDescriptor(mockDescriptor) >> true
        onDescriptorWriteSubject.onNext(new ByteAssociation<BluetoothGattDescriptor>(mockDescriptor, new byte[0]))

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

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
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

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
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertValueCount 1

        and:
        testSubscriber.assertValue dataFromCharacteristic
    }

    def "asObservable() not emit descriptor value if BLE notified only with non matching descriptor"() {

        given:
        givenDescriptorWithUUIDWritesData([descriptor: differentDescriptor, value: []])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

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
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertValueCount 1

        and:
        testSubscriber.assertValue secondValueFromCharacteristic
    }

    def "should release QueueReleaseInterface after successful write"() {

        given:
        givenDescriptorWithUUIDWritesData([descriptor: mockDescriptor, value: []])

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockQueueReleaseInterface.release()
    }

    def "should release QueueReleaseInterface when write failed to start"() {

        given:
        givenDescriptorWriteFailToStart()

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockQueueReleaseInterface.release()
    }

    def "should release QueueReleaseInterface when write failed"() {
        given:
        shouldEmitErrorOnDescriptorWrite(new Throwable("test"))

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockQueueReleaseInterface.release()
    }

    @Unroll
    def "should set parent BluetoothGattCharacteristic.writeType() to WRITE_TYPE_DEFAULT before calling BluetoothGatt.writeCharacteristic() and reset it to original value afterwards"() {

        given:
        mockGatt.writeDescriptor(mockDescriptor) >> writeStartSuccess

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockParentCharacteristic.setWriteType(bluetoothGattCharacteristicDefaultWriteType)

        then:
        1 * mockParentCharacteristic.setWriteType(originalParentBluetoothGattCharacteristicWriteType)

        where:
        writeStartSuccess << [ true, false ]
    }

    def "should timeout if RxBleGattCallback.onDescriptorWrite() won't trigger in 30 seconds"() {

        given:
        givenDescriptorWriteStartsOk()
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)

        then:
        testSubscriber.assertError(BleGattCallbackTimeoutException)

        and:
        testSubscriber.assertError {
            ((BleGattCallbackTimeoutException)it).getBleGattOperationType() == BleGattOperationType.DESCRIPTOR_WRITE
        }
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

    private givenDescriptorWriteStartsOk() {
        mockGatt.writeDescriptor(mockDescriptor) >> true
    }

    private prepareObjectUnderTest() {
        objectUnderTest = new DescriptorWriteOperation(mockCallback, mockGatt,
                new MockOperationTimeoutConfiguration(testScheduler), bluetoothGattCharacteristicDefaultWriteType, mockDescriptor, testData)
    }
}