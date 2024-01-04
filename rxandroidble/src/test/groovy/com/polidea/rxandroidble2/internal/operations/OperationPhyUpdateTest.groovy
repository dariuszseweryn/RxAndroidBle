package com.polidea.rxandroidble2.internal.operations

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble2.PhyPair
import com.polidea.rxandroidble2.RxBlePhy
import com.polidea.rxandroidble2.RxBlePhyOption
import com.polidea.rxandroidble2.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble2.exceptions.BleGattOperationType
import com.polidea.rxandroidble2.internal.RxBlePhyImpl
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble2.internal.util.MockOperationTimeoutConfiguration
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import spock.lang.Specification

import java.util.concurrent.TimeUnit

public class OperationPhyUpdateTest extends Specification {

    static long timeout = 10
    static TimeUnit timeoutTimeUnit = TimeUnit.SECONDS
    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface
    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt
    BluetoothDevice mockBluetoothDevice = Mock BluetoothDevice
    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback
    TestScheduler testScheduler = new TestScheduler()
    PublishSubject<PhyPair> updatedPhyPublishSubject = PublishSubject.create()
    PhyUpdateOperation objectUnderTest
    Set<RxBlePhy> rxSet = Set.of(RxBlePhy.PHY_1M)
    Set<RxBlePhy> txSet = Set.of(RxBlePhy.PHY_1M, RxBlePhy.PHY_2M)
    RxBlePhyOption phyOption = RxBlePhyOption.PHY_OPTION_S8

    def setup() {
        mockGattCallback.getOnPhyUpdate() >> updatedPhyPublishSubject
        mockBluetoothGatt.getDevice() >> mockBluetoothDevice
        mockBluetoothDevice.getAddress() >> "AA:BB:CC:DD:EE:FF"
        prepareObjectUnderTest()
    }

    def "should call BluetoothGatt.setPreferredPhy(int, int, int) exactly once when run()"() {

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockBluetoothGatt.setPreferredPhy(
                RxBlePhyImpl.enumSetToValuesMask(txSet),
                RxBlePhyImpl.enumSetToValuesMask(rxSet),
                phyOption.value
        )
    }

    def "should emit an error if RxBleGattCallback will emit error on RxBleGattCallback.getOnPhyUpdate() and release queue"() {

        given:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()
        def testException = new Exception("test")

        when:
        updatedPhyPublishSubject.onError(testException)

        then:
        testSubscriber.assertError(testException)

        and:
        (1.._) * mockQueueReleaseInterface.release() // technically it's not an error to call it more than once
    }

    def "should timeout if will not response after 10 seconds "() {

        given:
        println(objectUnderTest.toString())
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        testScheduler.advanceTimeTo(timeout + 5, timeoutTimeUnit)

        then:
        testSubscriber.assertError(BleGattCallbackTimeoutException)

        and:
        testSubscriber.assertError {
            ((BleGattCallbackTimeoutException)it).getBleGattOperationType() == BleGattOperationType.PHY_UPDATE
        }
    }

    private prepareObjectUnderTest() {
        objectUnderTest = new PhyUpdateOperation(mockGattCallback, mockBluetoothGatt,
                new MockOperationTimeoutConfiguration(timeout.intValue(), testScheduler), txSet, rxSet, phyOption)
    }
}
