package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble.exceptions.BleGattOperationType
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble.internal.util.MockOperationTimeoutConfiguration

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import rx.subjects.PublishSubject
import spock.lang.Specification

public class RxBleRadioOperationReadRssiTest extends Specification {

    Semaphore mockSemaphore = Mock Semaphore

    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt

    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback

    TestSubscriber<Integer> testSubscriber = new TestSubscriber()

    TestScheduler testScheduler = new TestScheduler()

    PublishSubject<Integer> onReadRemoteRssiPublishSubject = PublishSubject.create()

    RxBleRadioOperationReadRssi objectUnderTest

    def setup() {
        mockGattCallback.getOnRssiRead() >> onReadRemoteRssiPublishSubject
        prepareObjectUnderTest()
    }

    def "should call BluetoothGatt.readRemoteRssi() exactly once when run()"() {

        when:
        objectUnderTest.run()

        then:
        1 * mockBluetoothGatt.readRemoteRssi() >> true
    }

    def "should emit an error if BluetoothGatt.readRemoteRssi() returns false"() {

        given:
        mockBluetoothGatt.readRemoteRssi() >> false

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertError BleGattCannotStartException

        and:
        testSubscriber.assertError {
            it.getBleGattOperationType() == BleGattOperationType.READ_RSSI
        }

        and:
        1 * mockSemaphore.release()
    }

    def "should emit and error if RxBleGattCallback will emit error on getOnRssiRead() and release radio"() {

        given:
        mockBluetoothGatt.readRemoteRssi() >> true
        objectUnderTest.run()
        def testException = new Exception("test")

        when:
        onReadRemoteRssiPublishSubject.onError(testException)

        then:
        testSubscriber.assertError(testException)

        and:
        1 * mockSemaphore.release()
    }

    def "should emit exactly one value when RxBleGattCallback.getOnRssiRead() emits value"() {

        given:
        def rssi1 = 1
        def rssi2 = 2
        def rssi3 = 3
        mockBluetoothGatt.readRemoteRssi() >> true

        when:
        onReadRemoteRssiPublishSubject.onNext(rssi1)

        then:
        testSubscriber.assertNoValues()

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertNoValues()

        when:
        onReadRemoteRssiPublishSubject.onNext(rssi2)

        then:
        testSubscriber.assertValue(rssi2)

        and:
        1 * mockSemaphore.release()

        when:
        onReadRemoteRssiPublishSubject.onNext(rssi3)

        then:
        testSubscriber.assertValueCount(1) // no more values
    }

    def "should timeout if RxBleGattCallback.onReadRssi() won't trigger in 30 seconds"() {

        given:
        mockBluetoothGatt.readRemoteRssi() >> true
        objectUnderTest.run()

        when:
        testScheduler.advanceTimeBy(30, TimeUnit.SECONDS)

        then:
        testSubscriber.assertError(BleGattCallbackTimeoutException)

        and:
        testSubscriber.assertError {
            ((BleGattCallbackTimeoutException)it).getBleGattOperationType() == BleGattOperationType.READ_RSSI
        }
    }

    private prepareObjectUnderTest() {
        objectUnderTest = new RxBleRadioOperationReadRssi(mockGattCallback, mockBluetoothGatt,
                new MockOperationTimeoutConfiguration(testScheduler))
        objectUnderTest.setRadioBlockingSemaphore(mockSemaphore)
        objectUnderTest.asObservable().subscribe(testSubscriber)
    }
}