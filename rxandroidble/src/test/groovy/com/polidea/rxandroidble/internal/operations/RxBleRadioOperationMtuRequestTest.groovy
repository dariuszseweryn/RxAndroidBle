package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble.exceptions.BleGattOperationType
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble.internal.util.MockOperationTimeoutConfiguration
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import rx.subjects.PublishSubject
import spock.lang.Specification

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

public class RxBleRadioOperationMtuRequestTest extends Specification {

    static long timeout = 10
    static TimeUnit timeoutTimeUnit = TimeUnit.SECONDS
    Semaphore mockSemaphore = Mock Semaphore
    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt
    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback
    TestSubscriber<Integer> testSubscriber = new TestSubscriber()
    TestScheduler testScheduler = new TestScheduler()
    PublishSubject<Integer> changedMtuPublishSubject = PublishSubject.create()
    RxBleRadioOperationMtuRequest objectUnderTest
    int mtu = 72

    def setup() {
        mockGattCallback.getOnMtuChanged() >> changedMtuPublishSubject
        prepareObjectUnderTest()
    }

    def "should call BluetoothGatt.requestMtu(int) exactly once when run()"() {

        when:
        objectUnderTest.run()

        then:
        1 * mockBluetoothGatt.requestMtu(mtu) >> true
    }

    def "should emit an error if BluetoothGatt.requestMtu(int) returns false"() {

        given:
        mockBluetoothGatt.requestMtu(72) >> false

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertError BleGattCannotStartException

        and:
        testSubscriber.assertError {
            it.getBleGattOperationType() == BleGattOperationType.ON_MTU_CHANGED
        }

        and:
        1 * mockSemaphore.release()
    }

    def "should emit an error if RxBleGattCallback will emit error on RxBleGattCallback.getOnMtuChanged() and release radio"() {

        given:
        mockBluetoothGatt.requestMtu(72) >> true
        objectUnderTest.run()
        def testException = new Exception("test")

        when:
        changedMtuPublishSubject.onError(testException)

        then:
        testSubscriber.assertError(testException)

        and:
        (1.._) * mockSemaphore.release() // technically it's not an error to call it more than once
    }

    def "should timeout if will not response after 10 seconds "() {

        given:
        mockBluetoothGatt.requestMtu(72) >> true
        objectUnderTest.run()

        when:
        testScheduler.advanceTimeTo(timeout + 5, timeoutTimeUnit)

        then:
        testSubscriber.assertError(BleGattCallbackTimeoutException)

        and:
        testSubscriber.assertError {
            ((BleGattCallbackTimeoutException)it).getBleGattOperationType() == BleGattOperationType.ON_MTU_CHANGED
        }
    }

    private prepareObjectUnderTest() {
        objectUnderTest = new RxBleRadioOperationMtuRequest(mockGattCallback, mockBluetoothGatt,
                new MockOperationTimeoutConfiguration(timeout.intValue(), testScheduler), mtu)
        objectUnderTest.setRadioBlockingSemaphore(mockSemaphore)
        objectUnderTest.asObservable().subscribe(testSubscriber)
    }
}