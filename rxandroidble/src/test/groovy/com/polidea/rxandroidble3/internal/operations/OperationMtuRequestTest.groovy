package com.polidea.rxandroidble2.internal.operations

import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble2.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble2.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble2.exceptions.BleGattOperationType
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble2.internal.util.MockOperationTimeoutConfiguration
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import spock.lang.Specification

import java.util.concurrent.TimeUnit

public class OperationMtuRequestTest extends Specification {

    static long timeout = 10
    static TimeUnit timeoutTimeUnit = TimeUnit.SECONDS
    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface
    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt
    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback
    TestScheduler testScheduler = new TestScheduler()
    PublishSubject<Integer> changedMtuPublishSubject = PublishSubject.create()
    MtuRequestOperation objectUnderTest
    int mtu = 72

    def setup() {
        mockGattCallback.getOnMtuChanged() >> changedMtuPublishSubject
        prepareObjectUnderTest()
    }

    def "should call BluetoothGatt.requestMtu(int) exactly once when run()"() {

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockBluetoothGatt.requestMtu(mtu) >> true
    }

    def "should emit an error if BluetoothGatt.requestMtu(int) returns false"() {

        given:
        mockBluetoothGatt.requestMtu(72) >> false

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertError BleGattCannotStartException

        and:
        testSubscriber.assertError {
            it.getBleGattOperationType() == BleGattOperationType.ON_MTU_CHANGED
        }

        and:
        1 * mockQueueReleaseInterface.release()
    }

    def "should emit an error if RxBleGattCallback will emit error on RxBleGattCallback.getOnMtuChanged() and release queue"() {

        given:
        mockBluetoothGatt.requestMtu(72) >> true
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()
        def testException = new Exception("test")

        when:
        changedMtuPublishSubject.onError(testException)

        then:
        testSubscriber.assertError(testException)

        and:
        (1.._) * mockQueueReleaseInterface.release() // technically it's not an error to call it more than once
    }

    def "should timeout if will not response after 10 seconds "() {

        given:
        mockBluetoothGatt.requestMtu(72) >> true
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

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
        objectUnderTest = new MtuRequestOperation(mockGattCallback, mockBluetoothGatt,
                new MockOperationTimeoutConfiguration(timeout.intValue(), testScheduler), mtu)
    }
}