package com.polidea.rxandroidble.internal.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import com.polidea.rxandroidble.RxBleDeviceServices
import com.polidea.rxandroidble.internal.serialization.ConnectionOperationQueue
import com.polidea.rxandroidble.internal.operations.OperationsProvider
import com.polidea.rxandroidble.internal.operations.ServiceDiscoveryOperation
import java.util.concurrent.TimeUnit
import rx.Observable
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject
import spock.lang.Specification

class ServiceDiscoveryManagerTest extends Specification {
    def mockQueue = Mock ConnectionOperationQueue
    def mockBluetoothGatt = Mock BluetoothGatt
    def mockServiceDiscoveryOperationProvider = Mock OperationsProvider
    def TestSubscriber testSubscriber = new TestSubscriber()
    def TestSubscriber testSubscriber1 = new TestSubscriber()
    ServiceDiscoveryManager objectUnderTest = new ServiceDiscoveryManager(mockQueue, mockBluetoothGatt, mockServiceDiscoveryOperationProvider)

    def "should return services instantly if they were already discovered and are in BluetoothGatt cache"() {

        given:
        def servicesList = bluetoothGattContainsServices()

        when:
        objectUnderTest.getDiscoverServicesObservable(30, TimeUnit.SECONDS).subscribe(testSubscriber)

        then:
        testSubscriber.assertAnyOnNext { RxBleDeviceServices services -> services.getBluetoothGattServices() == servicesList }
        testSubscriber.assertCompleted()
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
        objectUnderTest.getDiscoverServicesObservable(timeout, timeoutTimeUnit).subscribe()

        then:
        1 * mockServiceDiscoveryOperationProvider.provideServiceDiscoveryOperation(timeout, timeoutTimeUnit) >> mockedOperation
        1 * mockQueue.queue(mockedOperation) >> Observable.empty()
    }

    def "should proxy services from ClientOperationQueue.queue()"() {

        given:
        bluetoothGattContainsNoServices()
        operationProviderProvidesOperation()
        def mockedDiscoveredServices = Mock(RxBleDeviceServices)
        mockQueue.queue(_) >> Observable.just(mockedDiscoveredServices)

        when:
        objectUnderTest.getDiscoverServicesObservable(5, TimeUnit.MILLISECONDS).subscribe(testSubscriber)

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
        objectUnderTest.getDiscoverServicesObservable(5, TimeUnit.MILLISECONDS).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(testThrowable)
    }

    def "should be able to run again if first operation failed"() {

        given:
        bluetoothGattContainsNoServices()
        operationProviderProvidesOperation()
        mockQueue.queue(_) >>> [Observable.error(new Throwable()), Observable.just(Mock(RxBleDeviceServices))]

        when:
        objectUnderTest.getDiscoverServicesObservable(5, TimeUnit.MILLISECONDS).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(Throwable)

        when:
        objectUnderTest.getDiscoverServicesObservable(5, TimeUnit.MILLISECONDS).subscribe(testSubscriber1)

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
        objectUnderTest.getDiscoverServicesObservable(5, TimeUnit.MILLISECONDS).subscribe(testSubscriber)
        objectUnderTest.getDiscoverServicesObservable(10, TimeUnit.MINUTES).subscribe(testSubscriber1)

        then:
        1 * mockQueue.queue(_) >> resultSubject
        testSubscriber.assertNotCompleted()
        testSubscriber1.assertNotCompleted()

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
        objectUnderTest.getDiscoverServicesObservable(5, TimeUnit.MILLISECONDS).subscribe(testSubscriber)

        then:
        1 * mockQueue.queue(_) >> Observable.just(result)
        testSubscriber.assertValue(result)

        when:
        objectUnderTest.getDiscoverServicesObservable(10, TimeUnit.MINUTES).subscribe(testSubscriber1)

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
