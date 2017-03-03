package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
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

public class RxBleRadioOperationDescriptorReadTest extends Specification {

    BluetoothGatt mockGatt = Mock BluetoothGatt

    RxBleGattCallback mockCallback = Mock RxBleGattCallback

    BluetoothGattDescriptor mockDescriptor = Mock BluetoothGattDescriptor

    BluetoothGattDescriptor differentDescriptor = Mock BluetoothGattDescriptor

    TestSubscriber<ByteAssociation<BluetoothGattDescriptor>> testSubscriber = new TestSubscriber()

    TestScheduler testScheduler = new TestScheduler()

    PublishSubject<ByteAssociation<BluetoothGattDescriptor>> onDescriptorReadSubject = PublishSubject.create()

    Semaphore mockSemaphore = Mock Semaphore

    RxBleRadioOperationDescriptorRead objectUnderTest

    def setup() {
        objectUnderTest = new RxBleRadioOperationDescriptorRead(mockCallback, mockGatt,
                new MockOperationTimeoutConfiguration(testScheduler), mockDescriptor)
        mockCallback.getOnDescriptorRead() >> onDescriptorReadSubject
        objectUnderTest.setRadioBlockingSemaphore(mockSemaphore)
        objectUnderTest.asObservable().subscribe(testSubscriber)
    }

    def "should call BluetoothGatt.readDescriptor() only once on single read when run()"() {

        when:
        objectUnderTest.run()

        then:
        1 * mockGatt.readDescriptor(mockDescriptor) >> true
    }

    def "asObservable() should not emit error when BluetoothGatt.readDescriptor() returns true while run()"() {

        given:
        givenDescriptorWithUUIDContainData([descriptor: mockDescriptor, value: []])

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertNoErrors()
    }

    def "asObservable() should emit error when BluetoothGatt.readDescriptor() returns false while run()"() {

        given:
        givenDescriptorReadFailToStart()

        when:
        objectUnderTest.run()

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
        objectUnderTest.run()

        then:
        testSubscriber.assertError testException
    }

    def "asObservable() should not emit when RxBleGattCallback.getOnDescriptorRead() emits before run()"() {
        // XXX [PU] I'm not sure if it is really desired
        given:
        mockGatt.readDescriptor(mockDescriptor) >> true
        onDescriptorReadSubject.onNext(ByteAssociation.create(mockDescriptor, new byte[0]))

        when:
        objectUnderTest.run()

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
        objectUnderTest.run()

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
        objectUnderTest.run()

        then:
        testSubscriber.assertValueCount 1

        and:
        testSubscriber.assertValue ByteAssociation.create(mockDescriptor, dataFromDescriptor)
    }

    def "asObservable() not emit descriptor value if BLE notified only with non matching descriptors"() {

        given:
        givenDescriptorWithUUIDContainData([descriptor: differentDescriptor, value: []])

        when:
        objectUnderTest.run()

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
        objectUnderTest.run()

        then:
        testSubscriber.assertValueCount 1

        and:
        testSubscriber.assertValue ByteAssociation.create(mockDescriptor, secondValueFromDescriptor)
    }

    def "should release Semaphore after successful read"() {

        given:
        givenDescriptorWithUUIDContainData([descriptor: mockDescriptor, value: []])

        when:
        objectUnderTest.run()

        then:
        1 * mockSemaphore.release()
    }

    def "should release Semaphore when read failed to start"() {

        given:
        givenDescriptorReadFailToStart()

        when:
        objectUnderTest.run()

        then:
        1 * mockSemaphore.release()
    }

    def "should release Semaphore when read failed"() {
        given:
        shouldEmitErrorOnDescriptorRead(new Throwable("test"))

        when:
        objectUnderTest.run()

        then:
        1 * mockSemaphore.release()
    }

    def "should timeout if RxBleGattCallback.onDescriptorRead() won't trigger in 30 seconds"() {

        given:
        givenDescriptorReadStartsOk()
        objectUnderTest.run()

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