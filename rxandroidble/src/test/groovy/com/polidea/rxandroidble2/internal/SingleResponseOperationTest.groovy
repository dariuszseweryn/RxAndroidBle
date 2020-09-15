package com.polidea.rxandroidble2.internal

import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble2.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble2.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble2.exceptions.BleGattOperationType
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble2.internal.operations.TimeoutConfiguration
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble2.internal.util.MockOperationTimeoutConfiguration
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

public class SingleResponseOperationTest extends Specification {

    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface
    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt
    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback
    TestScheduler testScheduler = new TestScheduler()
    PublishSubject<Object> callbackPublishSubject = PublishSubject.create()
    AtomicBoolean startOperationResult = new AtomicBoolean(false)

    @Shared
    Throwable testThrowable = new Throwable("test")

    @Shared
    Object testResult = new Object()

    TestSingleResponseOperation objectUnderTest
    TestObserver<Object> testObserver

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
        testObserver.assertValue(testResult)

        and:
        testObserver.assertComplete()
    }

    @Unroll
    def "should emit onError() with a proper BleGattOperationType when operation start will fail"() {

        given:
        prepareObjectUnderTest(operationType)
        givenWillStartOperationErroneously()

        when:
        subscribed()

        then:
        testObserver.assertError {
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
        testObserver.assertError {
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
        testObserver.assertError(testThrowable)
    }

    @Unroll
    def "should release queue after the callback will emit when unsubscribed"() {

        given:
        givenWillStartOperationSuccessfully()
        subscribed()

        when:
        testObserver.dispose()

        then:
        0 * mockQueueReleaseInterface.release()

        when:
        callbackResult.call(callbackPublishSubject)

        then:
        1 * mockQueueReleaseInterface.release()

        where:
        callbackResult << [
                { PublishSubject<Object> o -> o.onNext(testResult) },
                { PublishSubject<Object> o -> o.onError(testThrowable) }
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
        testObserver = objectUnderTest.run(mockQueueReleaseInterface).test()
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
        protected Single<Object> getCallback(RxBleGattCallback rxBleGattCallback) {
            return callbackSubject.firstOrError()
        }

        @Override
        protected boolean startOperation(BluetoothGatt bluetoothGatt) {
            return startOperationResult.get()
        }
    }
}