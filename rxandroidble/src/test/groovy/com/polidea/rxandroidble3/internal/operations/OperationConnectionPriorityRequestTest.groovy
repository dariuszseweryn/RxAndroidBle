package com.polidea.rxandroidble3.internal.operations

import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble3.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble3.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble3.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble3.internal.util.MockOperationTimeoutConfiguration
import io.reactivex.rxjava3.schedulers.TestScheduler
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class OperationConnectionPriorityRequestTest extends Specification {

    static long timeout = 10
    int completedDelay = 500L
    TimeUnit delayUnit = TimeUnit.MILLISECONDS
    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt
    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback
    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface
    TestScheduler testScheduler = new TestScheduler()
    TimeoutConfiguration mockTimeoutConfiguration = new MockOperationTimeoutConfiguration(
            timeout.intValue(),
            testScheduler
    )
    TimeoutConfiguration successTimeoutConfiguration = new TimeoutConfiguration(
            completedDelay,
            delayUnit,
            testScheduler
    )
    ConnectionPriorityChangeOperation objectUnderTest

    def setup() {
        prepareObjectUnderTest(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER)
    }

    def "should call BluetoothGatt.requestConnectionPriority(int) exactly once when run()"() {
        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockBluetoothGatt.requestConnectionPriority(_) >> true
    }

    def "should complete after specified time if BluetoothGatt.requestConnectionPriority() will return true"() {
        given:
        mockBluetoothGatt.requestConnectionPriority(_) >> true
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        testScheduler.advanceTimeBy(completedDelay + 500, delayUnit)

        then:
        testSubscriber.assertComplete()
    }

    def "should throw exception if operation failed"() {
        given:
        mockBluetoothGatt.requestConnectionPriority(_) >> false

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertError BleGattCannotStartException
    }

    def prepareObjectUnderTest(int connectionPriority) {
        objectUnderTest = new ConnectionPriorityChangeOperation(
                mockGattCallback,
                mockBluetoothGatt,
                mockTimeoutConfiguration,
                connectionPriority,
                successTimeoutConfiguration
        )
    }
}
