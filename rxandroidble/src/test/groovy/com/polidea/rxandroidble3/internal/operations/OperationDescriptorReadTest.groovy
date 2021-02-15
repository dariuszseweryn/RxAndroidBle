package com.polidea.rxandroidble3.internal.operations

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import com.polidea.rxandroidble3.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble3.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble3.exceptions.BleGattOperationType
import com.polidea.rxandroidble3.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble3.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble3.internal.util.ByteAssociation
import com.polidea.rxandroidble3.internal.util.MockOperationTimeoutConfiguration
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import spock.lang.Specification

import java.util.concurrent.TimeUnit

public class OperationDescriptorReadTest extends Specification {

    BluetoothGatt mockGatt = Mock BluetoothGatt
    RxBleGattCallback mockCallback = Mock RxBleGattCallback
    BluetoothGattDescriptor mockDescriptor = Mock BluetoothGattDescriptor
    BluetoothGattDescriptor differentDescriptor = Mock BluetoothGattDescriptor
    TestScheduler testScheduler = new TestScheduler()
    PublishSubject<ByteAssociation<BluetoothGattDescriptor>> onDescriptorReadSubject = PublishSubject.create()
    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface
    DescriptorReadOperation objectUnderTest

    def setup() {
        objectUnderTest = new DescriptorReadOperation(mockCallback, mockGatt,
                new MockOperationTimeoutConfiguration(testScheduler), mockDescriptor)
        mockCallback.getOnDescriptorRead() >> onDescriptorReadSubject
    }

    def "should call BluetoothGatt.readDescriptor() only once on single read when run()"() {

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockGatt.readDescriptor(mockDescriptor) >> true
    }

    def "asObservable() should not emit error when BluetoothGatt.readDescriptor() returns true while run()"() {

        given:
        givenDescriptorWithUUIDContainData([descriptor: mockDescriptor, value: []])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit error when BluetoothGatt.readDescriptor() returns false while run()"() {

        given:
        givenDescriptorReadFailToStart()

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertError BleGattCannotStartException

        and:
        testSubscriber.assertError {
            it.getBleGattOperationType() == BleGattOperationType.DESCRIPTOR_READ
        }
    }

    def "asObservable() should emit error when RxBleGattCallback.getOnDescriptorRead() emits error"() {

        given:
        Throwable testException = new Throwable("test")
        shouldEmitErrorOnDescriptorRead(testException)

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertError testException
    }

    def "asObservable() should not emit when RxBleGattCallback.getOnDescriptorRead() emits before run()"() {
        // XXX [PU] I'm not sure if it is really desired
        given:
        mockGatt.readDescriptor(mockDescriptor) >> true
        onDescriptorReadSubject.onNext(ByteAssociation.create(mockDescriptor, new byte[0]))

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertNoValues()

        and:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit next when RxBleGattCallback.getOnDescriptorRead() emits next"() {

        given:
        byte[] dataFromDescriptor = []
        givenDescriptorWithUUIDContainData([descriptor: mockDescriptor, value: dataFromDescriptor])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertValue ByteAssociation.create(mockDescriptor, dataFromDescriptor)
    }

    def "asObservable() emit only first descriptor value notified"() {

        given:
        byte[] dataFromDescriptor = [3, 4, 5]
        byte[] secondValueFromDescriptor = [1, 2, 3]
        givenDescriptorWithUUIDContainData(
                [descriptor: mockDescriptor, value: dataFromDescriptor],
                [descriptor: mockDescriptor, value: secondValueFromDescriptor])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertValueCount 1

        and:
        testSubscriber.assertValue ByteAssociation.create(mockDescriptor, dataFromDescriptor)
    }

    def "asObservable() not emit descriptor value if BLE notified only with non matching descriptors"() {

        given:
        givenDescriptorWithUUIDContainData([descriptor: differentDescriptor, value: []])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertValueCount 0
    }

    def "asObservable() emit only descriptor value for matching descriptor"() {

        given:
        byte[] dataFromDescriptor = [3, 4, 5]
        byte[] secondValueFromDescriptor = [1, 2, 3]
        givenDescriptorWithUUIDContainData(
                [descriptor: differentDescriptor, value: dataFromDescriptor],
                [descriptor: mockDescriptor, value: secondValueFromDescriptor])

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertValueCount 1

        and:
        testSubscriber.assertValue ByteAssociation.create(mockDescriptor, secondValueFromDescriptor)
    }

    def "should release QueueReleaseInterface after successful read"() {

        given:
        givenDescriptorWithUUIDContainData([descriptor: mockDescriptor, value: []])

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockQueueReleaseInterface.release()
    }

    def "should release QueueReleaseInterface when read failed to start"() {

        given:
        givenDescriptorReadFailToStart()

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockQueueReleaseInterface.release()
    }

    def "should release QueueReleaseInterface when read failed"() {
        given:
        shouldEmitErrorOnDescriptorRead(new Throwable("test"))

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockQueueReleaseInterface.release()
    }

    def "should timeout if RxBleGattCallback.onDescriptorRead() won't trigger in 30 seconds"() {

        given:
        givenDescriptorReadStartsOk()
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)

        then:
        testSubscriber.assertError(BleGattCallbackTimeoutException)

        and:
        testSubscriber.assertError {
            ((BleGattCallbackTimeoutException)it).getBleGattOperationType() == BleGattOperationType.DESCRIPTOR_READ
        }
    }

    private givenDescriptorWithUUIDContainData(Map... returnedDataOnRead) {
        mockGatt.readDescriptor(mockDescriptor) >> {
            returnedDataOnRead.each {
                onDescriptorReadSubject.onNext(new ByteAssociation(it['descriptor'], it['value'] as byte[]))
            }

            true
        }
    }

    private shouldEmitErrorOnDescriptorRead(Throwable testException) {
        mockGatt.readDescriptor(mockDescriptor) >> {
            onDescriptorReadSubject.onError(testException)
            true
        }
    }

    private givenDescriptorReadFailToStart() {
        mockGatt.readDescriptor(mockDescriptor) >> false
    }

    private givenDescriptorReadStartsOk() {
        mockGatt.readDescriptor(mockDescriptor) >> true
    }
}