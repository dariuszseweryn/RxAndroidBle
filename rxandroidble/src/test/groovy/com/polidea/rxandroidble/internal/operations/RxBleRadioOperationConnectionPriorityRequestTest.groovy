package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble.internal.RadioReleaseInterface
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble.internal.util.MockOperationTimeoutConfiguration
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import spock.lang.Specification
import java.util.concurrent.TimeUnit

class RxBleRadioOperationConnectionPriorityRequestTest extends Specification {

    static long timeout = 10
    int completedDelay = 500L
    TimeUnit delayUnit = TimeUnit.MILLISECONDS
    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt
    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback
    RadioReleaseInterface mockRadioReleaseInterface = Mock RadioReleaseInterface
    TestSubscriber<Integer> testSubscriber = new TestSubscriber()
    TestScheduler testScheduler = new TestScheduler()
    TimeoutConfiguration mockTimeoutConfiguration = new MockOperationTimeoutConfiguration(
            timeout.intValue(),
            testScheduler
    )
    RxBleRadioOperationConnectionPriorityRequest objectUnderTest

    def setup() {
        prepareObjectUnderTest(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER)
    }

    def "should call BluetoothGatt.requestConnectionPriority(int) exactly once when run()"() {
        when:
        objectUnderTest.run(mockRadioReleaseInterface).subscribe(testSubscriber)

        then:
        1 * mockBluetoothGatt.requestConnectionPriority(_) >> true
    }

    def "should complete after specified time if BluetoothGatt.requestConnectionPriority() will return true"() {
        given:
        mockBluetoothGatt.requestConnectionPriority(_) >> true
        objectUnderTest.run(mockRadioReleaseInterface).subscribe(testSubscriber)

        when:
        testScheduler.advanceTimeBy(completedDelay + 500, delayUnit)

        then:
        testSubscriber.assertCompleted()
    }

    def "should throw exception if operation failed"() {
        given:
        mockBluetoothGatt.requestConnectionPriority(_) >> false

        when:
        objectUnderTest.run(mockRadioReleaseInterface).subscribe(testSubscriber)

        then:
        testSubscriber.assertError BleGattCannotStartException
    }

    def prepareObjectUnderTest(int connectionPriority) {
        objectUnderTest = new RxBleRadioOperationConnectionPriorityRequest(
                mockGattCallback,
                mockBluetoothGatt,
                mockTimeoutConfiguration,
                connectionPriority,
                completedDelay,
                delayUnit,
                testScheduler
        )
    }
}
