package com.polidea.rxandroidble2.internal.operations

import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble2.PhyPair
import com.polidea.rxandroidble2.RxBlePhy
import com.polidea.rxandroidble2.RxBlePhyOption
import com.polidea.rxandroidble2.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble2.exceptions.BleGattOperationType
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble2.internal.util.MockOperationTimeoutConfiguration
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.PublishSubject
import spock.lang.Specification

import java.util.concurrent.TimeUnit

public class OperationPhyReadTest extends Specification {

    static long timeout = 10
    static TimeUnit timeoutTimeUnit = TimeUnit.SECONDS
    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface
    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt
    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback
    TestScheduler testScheduler = new TestScheduler()
    PublishSubject<PhyPair> readPhyPublishSubject = PublishSubject.create()
    PhyReadOperation objectUnderTest

    def setup() {
        mockGattCallback.getOnPhyRead() >> readPhyPublishSubject
        prepareObjectUnderTest()
    }

    def "should call BluetoothGatt.readPhy() exactly once when run()"() {

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockBluetoothGatt.readPhy()
    }

    def "should emit an error if RxBleGattCallback will emit error on RxBleGattCallback.getOnPhyRead() and release queue"() {

        given:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()
        def testException = new Exception("test")

        when:
        readPhyPublishSubject.onError(testException)

        then:
        testSubscriber.assertError(testException)

        and:
        (1.._) * mockQueueReleaseInterface.release() // technically it's not an error to call it more than once
    }

    def "should timeout if will not response after 10 seconds "() {

        given:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        testScheduler.advanceTimeTo(timeout + 5, timeoutTimeUnit)

        then:
        testSubscriber.assertError(BleGattCallbackTimeoutException)

        and:
        testSubscriber.assertError {
            ((BleGattCallbackTimeoutException)it).getBleGattOperationType() == BleGattOperationType.PHY_READ
        }
    }

    private prepareObjectUnderTest() {
        objectUnderTest = new PhyReadOperation(mockGattCallback, mockBluetoothGatt,
                new MockOperationTimeoutConfiguration(timeout.intValue(), testScheduler))
    }
}
