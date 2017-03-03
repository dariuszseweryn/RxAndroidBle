package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import com.polidea.rxandroidble.RxBleDeviceServices
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble.exceptions.BleGattOperationType
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble.internal.util.MockOperationTimeoutConfiguration

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import rx.Observable
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import rx.subjects.PublishSubject
import spock.lang.Specification

public class RxBleRadioOperationServicesDiscoverTest extends Specification {

    static long timeout = 20

    static TimeUnit timeoutTimeUnit = TimeUnit.SECONDS

    Semaphore mockSemaphore = Mock Semaphore

    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt

    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback

    TestSubscriber<RxBleDeviceServices> testSubscriber = new TestSubscriber()

    TestScheduler testScheduler = new TestScheduler()

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

    def "should timeout after specified amount of time if BluetoothGatt.getServices() will return empty list"() {

        given:
        mockBluetoothGatt.discoverServices() >> true
        mockGattCallback.onServicesDiscovered >> Observable.never()
        mockBluetoothGatt.getServices() >> []
        objectUnderTest.run()

        when:
        testScheduler.advanceTimeTo(timeout, timeoutTimeUnit)

        then:
        testSubscriber.assertError(BleGattCallbackTimeoutException)

        and:
        testSubscriber.assertError {
            ((BleGattCallbackTimeoutException)it).getBleGattOperationType() == BleGattOperationType.SERVICE_DISCOVERY
        }
    }

    def "should not timeout after specified amount of time if BluetoothGatt.getServices() will return non-empty list"() {

        given:
        mockBluetoothGatt.discoverServices() >> true
        mockGattCallback.onServicesDiscovered >> Observable.never()
        mockBluetoothGatt.getServices() >> createMockedBluetoothGattServiceList()
        objectUnderTest.run()

        when:
        testScheduler.advanceTimeTo(timeout, timeoutTimeUnit)

        then:
        testSubscriber.assertNoErrors()
    }

    def "should emit valid RxBleServices after specified timeout was reached if BluetoothGatt won't get onDiscoveryCompleted() callback (therefore no RxBleGattCallback.onServicesDiscovered() emission) if BluetoothGatt.getServices() will return not-empty list - should wait before emitting 5 more seconds"() {

        given:
        mockBluetoothGatt.discoverServices() >> true
        mockGattCallback.onServicesDiscovered >> Observable.never()
        def mockedBluetoothGattServiceList = createMockedBluetoothGattServiceList()
        mockBluetoothGatt.getServices() >> mockedBluetoothGattServiceList
        objectUnderTest.run()

        when:
        testScheduler.advanceTimeTo(timeout + 5, timeoutTimeUnit)

        then:
        testSubscriber.assertAnyOnNext { RxBleDeviceServices services ->
            services.bluetoothGattServices.containsAll(mockedBluetoothGattServiceList)
        }
    }

    private prepareObjectUnderTest() {
        objectUnderTest = new RxBleRadioOperationServicesDiscover(mockGattCallback, mockBluetoothGatt,
                new MockOperationTimeoutConfiguration(timeout.toInteger(), testScheduler))
        objectUnderTest.setRadioBlockingSemaphore(mockSemaphore)
        objectUnderTest.asObservable().subscribe(testSubscriber)
    }

    private List<BluetoothGattService> createMockedBluetoothGattServiceList() {
        return [Mock(BluetoothGattService), Mock(BluetoothGattService)]
    }
}