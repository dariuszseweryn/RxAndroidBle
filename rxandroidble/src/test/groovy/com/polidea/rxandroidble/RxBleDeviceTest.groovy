package com.polidea.rxandroidble

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTED
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTING
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.DISCONNECTED

import android.bluetooth.BluetoothDevice
import android.content.Context
import rx.Observable
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject
import spock.lang.Specification
import spock.lang.Unroll

public class RxBleDeviceTest extends Specification {

    BluetoothDevice mockBluetoothDevice = Mock BluetoothDevice

    RxBleConnection.Connector mockConnector = Mock RxBleConnection.Connector

    PublishSubject<RxBleConnection> mockConnectorEstablishConnectionPublishSubject = PublishSubject.create()

    RxBleConnection mockConnection = Mock RxBleConnection

    PublishSubject<RxBleConnection.RxBleConnectionState> connectionStatePublishSubject = PublishSubject.create()

    RxBleDevice rxBleDevice = new RxBleDeviceImpl(mockBluetoothDevice, mockConnector)

    TestSubscriber deviceConnectionStateSubscriber = new TestSubscriber()

    def setup() {
        mockConnector.prepareConnection(_, _) >> mockConnectorEstablishConnectionPublishSubject
        mockConnection.getConnectionState() >> connectionStatePublishSubject
    }

    def "should return the BluetoothDevice name"() {

        given:
        mockBluetoothDevice.name >> "testName"

        expect:
        rxBleDevice.getName() == "testName"
    }

    def "should return the BluetoothDevice address"() {

        given:
        mockBluetoothDevice.address >> "aa:aa:aa:aa:aa:aa"

        expect:
        rxBleDevice.getMacAddress() == "aa:aa:aa:aa:aa:aa"
    }

    def "equals() should return true when compared to a different RxBleDevice instance with the same underlying BluetoothDevice"() {

        given:
        def differentRxBleDevice = new RxBleDeviceImpl(mockBluetoothDevice, null)

        expect:
        rxBleDevice.equals(differentRxBleDevice)
    }

    def "hashCode() should return the same value as a different RxBleDevice instance hashCode() with the same underlying BluetoothDevice"() {

        given:
        def differentRxBleDevice = new RxBleDeviceImpl(mockBluetoothDevice, null)

        expect:
        rxBleDevice.hashCode() == differentRxBleDevice.hashCode()
    }

    @Unroll
    def "establishConnection() should call RxBleConnection.Connector.prepareConnection() #id"() {

        when:
        rxBleDevice.establishConnection(theContext, theAutoConnectValue).subscribe()

        then:
        1 * mockConnector.prepareConnection(theContext, theAutoConnectValue) >> connectionStatePublishSubject

        where:
        theContext    | theAutoConnectValue
        null          | true
        null          | false
        Mock(Context) | true
        Mock(Context) | false
    }

    def "getConnectionState() should emit DISCONNECTED when subscribed and RxBleDevice was not connected yet"() {

        when:
        rxBleDevice.getConnectionState().subscribe(deviceConnectionStateSubscriber)

        then:
        deviceConnectionStateSubscriber.assertValue(DISCONNECTED)
    }

    def "getConnectionState() should emit CONNECTING when subscribed and establishConnection() was subscribed"() {

        given:
        startConnecting()

        when:
        rxBleDevice.getConnectionState().subscribe(deviceConnectionStateSubscriber)

        then:
        deviceConnectionStateSubscriber.assertValue(CONNECTING)
    }

    def "getConnectionState() should emit DISCONNECTED, CONNECTING state on subscribing to establishConnection()"() {
        given:
        rxBleDevice.getConnectionState().subscribe(deviceConnectionStateSubscriber)

        when:
        startConnecting()

        then:
        deviceConnectionStateSubscriber.assertValues(DISCONNECTED, CONNECTING)
    }

    def "getConnectionState() should emit CONNECTED when subscribed after establishConnection() has emitted"() {

        given:
        startConnecting()
        connectingSuccess()

        when:
        rxBleDevice.getConnectionState().subscribe(deviceConnectionStateSubscriber)

        then:
        deviceConnectionStateSubscriber.assertValue(CONNECTED)
    }

    def "getConnectionState() should emit CONNECTING and CONNECTED state when subscribed after subscribing establishConnection() and before it emits RxBleConnection"() {

        given:
        startConnecting()
        rxBleDevice.getConnectionState().subscribe(deviceConnectionStateSubscriber)

        when:
        connectingSuccess()

        then:
        deviceConnectionStateSubscriber.assertValues(CONNECTING, CONNECTED)
    }

    def "getConnectionState() should emit DISCONNECTED state on unsubscribing from establishConnection()"() {

        given:
        def connectionTestSubscriber = new TestSubscriber()
        rxStartConnecting().subscribe(connectionTestSubscriber)
        connectingSuccess()
        connectionTestSubscriber.unsubscribe()

        when:
        rxBleDevice.getConnectionState().subscribe(deviceConnectionStateSubscriber)

        then:
        deviceConnectionStateSubscriber.assertValue(DISCONNECTED)
    }

    def "getConnectionState() should not propagate RxBleConnection.getConnectionState() errors"() {
        given:
        rxBleDevice.getConnectionState().subscribe(deviceConnectionStateSubscriber)
        startConnecting()
        connectingSuccess()

        when:
        emitConnectionStateError()

        then:
        deviceConnectionStateSubscriber.assertNoErrors()

        and:
        deviceConnectionStateSubscriber.assertValues(DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTED)
    }
    
    public void startConnecting() {
        rxBleDevice.establishConnection(null, false).subscribe()
    }

    public Observable<RxBleConnection> rxStartConnecting() {
        return rxBleDevice.establishConnection(null, false)
    }

    public void connectingSuccess() {
        mockConnectorEstablishConnectionPublishSubject.onNext(mockConnection)
    }

    public void emitConnectionStateError() {
        connectionStatePublishSubject.onError(new Throwable("test"))
    }
}
