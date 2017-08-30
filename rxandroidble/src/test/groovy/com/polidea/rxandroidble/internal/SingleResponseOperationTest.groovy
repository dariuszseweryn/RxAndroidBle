package com.polidea.rxandroidble.internal

import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble.exceptions.BleGattOperationType
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble.internal.operations.TimeoutConfiguration
import com.polidea.rxandroidble.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble.internal.util.MockOperationTimeoutConfiguration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import rx.Observable
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import rx.subjects.PublishSubject
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

public class SingleResponseOperationTest extends Specification {

    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface

    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt

    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback

    TestSubscriber<Object> testSubscriber = new TestSubscriber()

    TestScheduler testScheduler = new TestScheduler()

    PublishSubject<Object> callbackPublishSubject = PublishSubject.create()

    AtomicBoolean startOperationResult = new AtomicBoolean(false)

    @Shared Throwable testThrowable = new Throwable("test")

    @Shared Object testResult = new Object()

    TestSingleResponseOperation objectUnderTest

    def setup() {
        prepareObjectUnderTest(BleGattOperationType.DESCRIPTOR_READ)
    }

    def "should emit onNext and onComplete on happy path"() {

        given:
        givenWillStartOperationSuccessfully()
        subscribed()

        when:
        callbackPublishSubject.onNext(testResult)

        then:
        testSubscriber.assertValue(testResult)

        and:
        testSubscriber.assertCompleted()
    }

    @Unroll
    def "should emit onError() with a proper BleGattOperationType when operation start will fail"() {

        given:
        prepareObjectUnderTest(operationType)
        givenWillStartOperationErroneously()

        when:
        subscribed()

        then:
        testSubscriber.assertError {
            BleGattCannotStartException e -> e.bleGattOperationType == operationType
        }

        where:
        operationType << [
                BleGattOperationType.CHARACTERISTIC_CHANGED,
                BleGattOperationType.CHARACTERISTIC_LONG_WRITE,
                BleGattOperationType.CHARACTERISTIC_READ,
                BleGattOperationType.CHARACTERISTIC_WRITE,
                BleGattOperationType.CONNECTION_PRIORITY_CHANGE,
                BleGattOperationType.CONNECTION_STATE,
                BleGattOperationType.DESCRIPTOR_READ
        ]
    }

    @Unroll
    def "should emit onError() with a proper BleGattOperationType when operation callback will timeout"() {

        given:
        prepareObjectUnderTest(operationType)
        givenWillStartOperationSuccessfully()
        subscribed()

        when:
        testScheduler.advanceTimeTo(MockOperationTimeoutConfiguration.TIMEOUT_IN_SEC, TimeUnit.SECONDS)

        then:
        testSubscriber.assertError {
            BleGattCallbackTimeoutException e -> e.bleGattOperationType == operationType
        }

        where:
        operationType << [
                BleGattOperationType.CHARACTERISTIC_CHANGED,
                BleGattOperationType.CHARACTERISTIC_LONG_WRITE,
                BleGattOperationType.CHARACTERISTIC_READ,
                BleGattOperationType.CHARACTERISTIC_WRITE,
                BleGattOperationType.CONNECTION_PRIORITY_CHANGE,
                BleGattOperationType.CONNECTION_STATE,
                BleGattOperationType.DESCRIPTOR_READ
        ]
    }

    def "should pass to onError() throwable from callback"() {

        given:
        givenWillStartOperationSuccessfully()
        subscribed()

        when:
        callbackPublishSubject.onError(testThrowable)

        then:
        testSubscriber.assertError(testThrowable)
    }

    @Unroll
    def "should release queue after the callback will emit when unsubscribed"() {

        given:
        givenWillStartOperationSuccessfully()
        subscribed()

        when:
        testSubscriber.unsubscribe()

        then:
        0 * mockQueueReleaseInterface.release()

        when:
        callbackResult.call(callbackPublishSubject)

        then:
        1 * mockQueueReleaseInterface.release()

        where:
        callbackResult << [
                { rx.Observer<Object> o -> o.onNext(testResult) },
                { rx.Observer<Object> o -> o.onError(testThrowable) }
        ]
    }

    private prepareObjectUnderTest(BleGattOperationType bleGattOperationType) {
        objectUnderTest = new TestSingleResponseOperation(mockBluetoothGatt, mockGattCallback, bleGattOperationType,
                new MockOperationTimeoutConfiguration(testScheduler), callbackPublishSubject, startOperationResult)
    }

    private givenWillStartOperationSuccessfully() {
        startOperationResult.set(true)
    }

    private givenWillStartOperationErroneously() {
        startOperationResult.set(false)
    }

    private subscribed() {
        objectUnderTest.run(mockQueueReleaseInterface).subscribe(testSubscriber)
    }

    private static class TestSingleResponseOperation extends SingleResponseOperation<Object> {

        private final PublishSubject<Object> callbackSubject

        private final AtomicBoolean startOperationResult

        TestSingleResponseOperation(
                BluetoothGatt bluetoothGatt,
                RxBleGattCallback rxBleGattCallback,
                BleGattOperationType bleGattOperationType,
                TimeoutConfiguration timeoutConfiguration,
                PublishSubject<Object> callbackSubject,
                AtomicBoolean startOperationResult
        ) {
            super(bluetoothGatt, rxBleGattCallback, bleGattOperationType, timeoutConfiguration)
            this.startOperationResult = startOperationResult
            this.callbackSubject = callbackSubject
        }

        @Override
        protected Observable<Object> getCallback(RxBleGattCallback rxBleGattCallback) {
            return callbackSubject
        }

        @Override
        protected boolean startOperation(BluetoothGatt bluetoothGatt) {
            return startOperationResult.get()
        }
    }
}