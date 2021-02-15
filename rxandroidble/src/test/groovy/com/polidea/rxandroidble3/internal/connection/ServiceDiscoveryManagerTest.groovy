package com.polidea.rxandroidble3.internal.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import com.polidea.rxandroidble3.RxBleDeviceServices
import com.polidea.rxandroidble3.internal.operations.OperationsProvider
import com.polidea.rxandroidble3.internal.operations.ServiceDiscoveryOperation
import com.polidea.rxandroidble3.internal.serialization.ConnectionOperationQueue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class ServiceDiscoveryManagerTest extends Specification {
    def mockQueue = Mock ConnectionOperationQueue
    def mockBluetoothGatt = Mock BluetoothGatt
    def mockServiceDiscoveryOperationProvider = Mock OperationsProvider
    ServiceDiscoveryManager objectUnderTest = new ServiceDiscoveryManager(mockQueue, mockBluetoothGatt, mockServiceDiscoveryOperationProvider)

    def "should return services instantly if they were already discovered and are in BluetoothGatt cache"() {

        given:
        def servicesList = bluetoothGattContainsServices()

        when:
        def testSubscriber = objectUnderTest.getDiscoverServicesSingle(30, TimeUnit.SECONDS).test()

        then:
        testSubscriber.assertAnyOnNext { RxBleDeviceServices services -> services.getBluetoothGattServices() == servicesList }
        testSubscriber.assertComplete()
        0 * mockServiceDiscoveryOperationProvider.provideServiceDiscoveryOperation(_, _)
        0 * mockQueue.queue(_)
    }

    def "should try to discover services if there are no services cached within BluetoothGatt"() {

        given:
        bluetoothGattContainsNoServices()
        def timeout = 5
        def timeoutTimeUnit = TimeUnit.MILLISECONDS
        def mockedOperation = Mock(ServiceDiscoveryOperation)

        when:
        def testSubscriber = objectUnderTest.getDiscoverServicesSingle(timeout, timeoutTimeUnit).test()

        then:
        testSubscriber.assertNoErrors()
        1 * mockServiceDiscoveryOperationProvider.provideServiceDiscoveryOperation(timeout, timeoutTimeUnit) >> mockedOperation
        1 * mockQueue.queue(mockedOperation) >> Observable.just(new RxBleDeviceServices(Arrays.asList()))
    }

    def "should proxy services from ClientOperationQueue.queue()"() {

        given:
        bluetoothGattContainsNoServices()
        operationProviderProvidesOperation()
        def mockedDiscoveredServices = Mock(RxBleDeviceServices)
        mockQueue.queue(_) >> Observable.just(mockedDiscoveredServices)

        when:
        def testSubscriber = objectUnderTest.getDiscoverServicesSingle(5, TimeUnit.MILLISECONDS).test()

        then:
        testSubscriber.assertValue(mockedDiscoveredServices)
    }

    def "should proxy exceptions from ClientOperationQueue.queue()"() {

        given:
        bluetoothGattContainsNoServices()
        operationProviderProvidesOperation()
        Throwable testThrowable = new Throwable("test")
        mockQueue.queue(_) >> Observable.error(testThrowable)

        when:
        def testSubscriber = objectUnderTest.getDiscoverServicesSingle(5, TimeUnit.MILLISECONDS).test()

        then:
        testSubscriber.assertError(testThrowable)
    }

    def "should be able to run again if first operation failed"() {

        given:
        bluetoothGattContainsNoServices()
        operationProviderProvidesOperation()
        mockQueue.queue(_) >>> [Observable.error(new Throwable()), Observable.just(Mock(RxBleDeviceServices))]

        when:
        def testSubscriber = objectUnderTest.getDiscoverServicesSingle(5, TimeUnit.MILLISECONDS).test()

        then:
        testSubscriber.assertError(Throwable)

        when:
        def testSubscriber1 = objectUnderTest.getDiscoverServicesSingle(5, TimeUnit.MILLISECONDS).test()

        then:
        testSubscriber1.assertNoErrors()
        testSubscriber1.assertValueCount(1)
    }

    def "should share the same action if more subscribers wait for results"() {

        given:
        bluetoothGattContainsNoServices()
        operationProviderProvidesOperation()
        PublishSubject<RxBleDeviceServices> resultSubject = PublishSubject.create()
        RxBleDeviceServices result = Mock(RxBleDeviceServices)

        when:
        def testSubscriber = objectUnderTest.getDiscoverServicesSingle(5, TimeUnit.MILLISECONDS).test()
        def testSubscriber1 = objectUnderTest.getDiscoverServicesSingle(10, TimeUnit.MINUTES).test()

        then:
        1 * mockQueue.queue(_) >> resultSubject
        testSubscriber.assertNotComplete()
        testSubscriber1.assertNotComplete()

        when:
        resultSubject.onNext(result)

        then:
        testSubscriber.assertValue(result)
        testSubscriber1.assertValue(result)
    }

    def "should cache the result if more subscribers would request services later"() {

        given:
        bluetoothGattContainsNoServices()
        operationProviderProvidesOperation()
        RxBleDeviceServices result = Mock(RxBleDeviceServices)

        when:
        def testSubscriber = objectUnderTest.getDiscoverServicesSingle(5, TimeUnit.MILLISECONDS).test()

        then:
        1 * mockQueue.queue(_) >> Observable.just(result)
        testSubscriber.assertValue(result)

        when:
        def testSubscriber1 = objectUnderTest.getDiscoverServicesSingle(10, TimeUnit.MINUTES).test()

        then:
        0 * mockQueue.queue(_)
        testSubscriber1.assertValue(result)
    }

    private List<BluetoothGattService> bluetoothGattContainsServices() {
        def servicesList = Arrays.asList(Mock(BluetoothGattService))
        mockBluetoothGatt.getServices() >> servicesList
        return servicesList
    }

    private void bluetoothGattContainsNoServices() {
        mockBluetoothGatt.getServices() >> Arrays.asList()
    }

    private void operationProviderProvidesOperation() {
        mockServiceDiscoveryOperationProvider.provideServiceDiscoveryOperation(_, _) >> Mock(ServiceDiscoveryOperation)
    }
}
