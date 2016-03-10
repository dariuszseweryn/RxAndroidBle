package com.polidea.rxandroidble.internal.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import com.polidea.rxandroidble.FlatRxBleRadio
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.RxBleDeviceServices
import com.polidea.rxandroidble.exceptions.BleGattException
import com.polidea.rxandroidble.exceptions.BleGattOperationType
import rx.observers.TestSubscriber
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import spock.lang.Specification

import static java.util.Collections.emptyList
import static rx.Observable.just

class RxBleConnectionTest extends Specification {
    def flatRadio = new FlatRxBleRadio()
    def callback = Mock RxBleGattCallback
    def bluetoothGattMock = Mock BluetoothGatt
    def objectUnderTest = new RxBleConnectionImpl(flatRadio, callback, bluetoothGattMock)
    def connectionStateChange = BehaviorSubject.create()
    def TestSubscriber testSubscriber

    def setup() {
        testSubscriber = new TestSubscriber()
        callback.getOnConnectionStateChange() >> connectionStateChange
    }

    def "should proxy connection state"() {
        given:
        connectionStateChange.onNext(pushedStatus)

        when:
        objectUnderTest.getConnectionState().subscribe(testSubscriber)

        then:
        testSubscriber.assertValue(expectedState)

        where:
        pushedStatus                                       | expectedState
        RxBleConnection.RxBleConnectionState.CONNECTED     | RxBleConnection.RxBleConnectionState.CONNECTED
        RxBleConnection.RxBleConnectionState.DISCONNECTED  | RxBleConnection.RxBleConnectionState.DISCONNECTED
        RxBleConnection.RxBleConnectionState.CONNECTING    | RxBleConnection.RxBleConnectionState.CONNECTING
        RxBleConnection.RxBleConnectionState.DISCONNECTING | RxBleConnection.RxBleConnectionState.DISCONNECTING
    }

    def "should emit error if failed to start retrieving services"() {
        given:
        callback.getOnServicesDiscovered() >> PublishSubject.create()
        bluetoothGattMock.getServices() >> emptyList()
        shouldFailStartingDiscovery()

        when:
        objectUnderTest.discoverServices().subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleGattException)
        testSubscriber.assertErrorClosure {
            it.bleGattOperationType == BleGattOperationType.SERVICE_DISCOVERY
        }
    }

    def "should return cached services"() {
        given:
        def services = [Mock(BluetoothGattService), Mock(BluetoothGattService)]
        shouldSuccessfullyStartDiscovery()
        objectUnderTest.discoverServices().subscribe()

        when:
        objectUnderTest.discoverServices().subscribe(testSubscriber)

        then:
        testSubscriber.assertServices services
        testSubscriber.assertCompleted()
        (_..1) * bluetoothGattMock.getServices() >> emptyList()
        1 * callback.getOnServicesDiscovered() >> just(new RxBleDeviceServices(services))
    }

    def "should return services instantly if they were already discovered"() {
        given:
        def services = [Mock(BluetoothGattService), Mock(BluetoothGattService)]
        bluetoothGattMock.getServices() >> services

        when:
        objectUnderTest.discoverServices().subscribe(testSubscriber)

        then:
        testSubscriber.assertServices services
        testSubscriber.assertCompleted()
        0 * bluetoothGattMock.discoverServices()
    }

    def "should try to discover services if cached services are empty"() {
        given:
        def services = [Mock(BluetoothGattService), Mock(BluetoothGattService)]
        shouldSuccessfullyStartDiscovery()
        bluetoothGattMock.getServices() >> emptyList()
        callback.getOnServicesDiscovered() >> just(new RxBleDeviceServices(services))

        when:
        objectUnderTest.discoverServices().subscribe(testSubscriber)

        then:
        testSubscriber.assertServices services
        testSubscriber.assertCompleted()
        1 * bluetoothGattMock.discoverServices() >> true
    }

    public shouldFailStartingDiscovery() {
        bluetoothGattMock.discoverServices() >> false
    }

    public shouldSuccessfullyStartDiscovery() {
        bluetoothGattMock.discoverServices() >> true
    }

    public shouldGattCallbackReturnServicesOnDiscovery(ArrayList<BluetoothGattService> services) {
        bluetoothGattMock.discoverServices() >> true
        callback.getOnServicesDiscovered() >> just(new RxBleDeviceServices(services))
    }
}
