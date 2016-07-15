package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble.RxBleDeviceServices
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble.exceptions.BleGattOperationType
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import java.util.concurrent.Semaphore
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject
import spock.lang.Specification

public class RxBleRadioOperationServicesDiscoverTest extends Specification {

    Semaphore mockSemaphore = Mock Semaphore

    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt

    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback

    TestSubscriber<RxBleDeviceServices> testSubscriber = new TestSubscriber()

    PublishSubject<RxBleDeviceServices> onServicesDiscoveredPublishSubject = PublishSubject.create()

    RxBleRadioOperationServicesDiscover objectUnderTest

    def setup() {
        mockGattCallback.getOnServicesDiscovered() >> onServicesDiscoveredPublishSubject
        prepareObjectUnderTest()
    }

    def "should call BluetoothGatt.discoverServices() exactly once when run()"() {

        when:
        objectUnderTest.run()

        then:
        1 * mockBluetoothGatt.discoverServices() >> true
    }

    def "should emit an error if BluetoothGatt.discoverServices() returns false"() {

        given:
        mockBluetoothGatt.discoverServices() >> false

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertError BleGattCannotStartException

        and:
        testSubscriber.assertError {
            it.getBleGattOperationType() == BleGattOperationType.SERVICE_DISCOVERY
        }

        and:
        1 * mockSemaphore.release()
    }

    def "should emit an error if RxBleGattCallback will emit error on RxBleGattCallback.getOnServicesDiscovered() and release radio"() {

        given:
        mockBluetoothGatt.discoverServices() >> true
        objectUnderTest.run()
        def testException = new Exception("test")

        when:
        onServicesDiscoveredPublishSubject.onError(testException)

        then:
        testSubscriber.assertError(testException)

        and:
        (1.._) * mockSemaphore.release() // technically it's not an error to call it more than once
    }

    def "should emit exactly one value when RxBleGattCallback.getOnServicesDiscovered() emits value"() {

        given:
        def value1 = new RxBleDeviceServices(new ArrayList<>())
        def value2 = new RxBleDeviceServices(new ArrayList<>())
        def value3 = new RxBleDeviceServices(new ArrayList<>())
        mockBluetoothGatt.discoverServices() >> true

        when:
        onServicesDiscoveredPublishSubject.onNext(value1)

        then:
        testSubscriber.assertNoValues()

        when:
        objectUnderTest.run()

        then:
        testSubscriber.assertNoValues()

        when:
        onServicesDiscoveredPublishSubject.onNext(value2)

        then:
        testSubscriber.assertValue(value2)

        and:
        1 * mockSemaphore.release()

        when:
        onServicesDiscoveredPublishSubject.onNext(value3)

        then:
        testSubscriber.assertValueCount(1) // no more values
    }

    private prepareObjectUnderTest() {
        objectUnderTest = new RxBleRadioOperationServicesDiscover(mockGattCallback, mockBluetoothGatt)
        objectUnderTest.setRadioBlockingSemaphore(mockSemaphore)
        objectUnderTest.asObservable().subscribe(testSubscriber)
    }
}