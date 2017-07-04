package com.polidea.rxandroidble.internal

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble.ConnectionSetup
import com.jakewharton.rxrelay.BehaviorRelay
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.RxBleDevice
import com.polidea.rxandroidble.exceptions.BleAlreadyConnectedException
import com.polidea.rxandroidble.exceptions.BleGattException
import com.polidea.rxandroidble.exceptions.BleGattOperationType
import com.polidea.rxandroidble.internal.connection.Connector
import rx.Observable
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.*

public class RxBleDeviceTest extends Specification {

    BluetoothDevice mockBluetoothDevice = Mock BluetoothDevice
    Connector mockConnector = Mock Connector
    RxBleConnection mockConnection = Mock RxBleConnection
    PublishSubject<RxBleConnection> mockConnectorEstablishConnectionPublishSubject = PublishSubject.create()
    BehaviorRelay<RxBleConnection.RxBleConnectionState> connectionStateBehaviorRelay = BehaviorRelay.create(DISCONNECTED)
    @Shared BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt
    RxBleDevice rxBleDevice = new RxBleDeviceImpl(mockBluetoothDevice, mockConnector, connectionStateBehaviorRelay)
    TestSubscriber deviceConnectionStateSubscriber = new TestSubscriber()

    def setup() {
        mockConnector.prepareConnection(_) >> mockConnectorEstablishConnectionPublishSubject
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
        def differentRxBleDeviceWithSameBluetoothDevice = new RxBleDeviceImpl(mockBluetoothDevice, null, BehaviorRelay.create())

        expect:
        rxBleDevice == differentRxBleDeviceWithSameBluetoothDevice
    }

    def "hashCode() should return the same value as a different RxBleDevice instance hashCode() with the same underlying BluetoothDevice"() {

        given:
        def differentRxBleDevice = new RxBleDeviceImpl(mockBluetoothDevice, null, BehaviorRelay.create())

        expect:
        rxBleDevice.hashCode() == differentRxBleDevice.hashCode()
    }

    @Unroll
    def "establishConnection() should call RxBleConnection.Connector.prepareConnection() #id"() {

        when:
        rxBleDevice.establishConnection(theAutoConnectValue).subscribe()

        then:
        1 * mockConnector.prepareConnection({ ConnectionSetup cs -> cs.autoConnect == theAutoConnectValue }) >> Observable.empty()

        where:
        theAutoConnectValue << [true, false]
    }

    @Unroll
    def "should emit only new states from BehaviourRelay<RxBleConnectionState>"() {

        given:
        connectionStateBehaviorRelay.call(initialState)
        rxBleDevice.observeConnectionStateChanges().subscribe(deviceConnectionStateSubscriber)

        when:
        connectionStateBehaviorRelay.call(initialState)

        then:
        deviceConnectionStateSubscriber.assertNoValues()

        when:
        connectionStateBehaviorRelay.call(nextState)

        then:
        deviceConnectionStateSubscriber.assertValue(nextState)

        where:
        initialState  | nextState
        DISCONNECTED  | CONNECTING
        DISCONNECTED  | CONNECTED
        DISCONNECTED  | DISCONNECTING
        CONNECTING    | CONNECTED
        CONNECTING    | DISCONNECTING
        CONNECTING    | DISCONNECTED
        CONNECTED     | CONNECTING
        CONNECTED     | DISCONNECTING
        CONNECTED     | DISCONNECTED
        DISCONNECTING | DISCONNECTED
        DISCONNECTING | CONNECTING
        DISCONNECTING | CONNECTED
    }

    def "should emit connection and stay subscribed after it was established"() {

        given:
        def testSubscriber = new TestSubscriber()

        when:
        rxStartConnecting().subscribe(testSubscriber)
        notifyConnectionWasEstablished()

        then:
        testSubscriber.assertSubscribed()
        testSubscriber.assertValueCount 1
    }

    def "should emit error if already connected"() {

        given:
        def testSubscriber = new TestSubscriber()
        rxStartConnecting().subscribe()
        notifyConnectionWasEstablished()

        when:
        rxStartConnecting().subscribe(testSubscriber)

        then:
        testSubscriber.assertError BleAlreadyConnectedException
    }

    def "should create new connection if previous connection was established and released before second subscriber has subscribed"() {

        given:
        def firstSubscriber = new TestSubscriber()
        def secondSubscriber = new TestSubscriber()
        def subscription = rxStartConnecting().subscribe(firstSubscriber)
        notifyConnectionWasEstablished()
        subscription.unsubscribe()

        when:
        rxBleDevice.establishConnection(false).subscribe(secondSubscriber)

        then:
        firstSubscriber.assertValueCount 1
        firstSubscriber.assertReceivedOnNextNot(secondSubscriber.onNextEvents)
    }

    def "should unsubscribe from connection if it was dropped"() {

        given:
        def connectionTestSubscriber = new TestSubscriber()
        rxStartConnecting().subscribe(connectionTestSubscriber)
        notifyConnectionWasEstablished()

        when:
        dropConnection()

        then:
        connectionTestSubscriber.isUnsubscribed()
    }

    @Unroll
    def "should return connection state from BehaviourRelay<RxBleConnectionState>"() {

        given:
        connectionStateBehaviorRelay.call(state)

        expect:
        rxBleDevice.getConnectionState() == state

        where:
        state << [DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING]
    }

    def "should return initial BluetoothDevice on getBluetoothDevice()"() {

        expect:
        rxBleDevice.getBluetoothDevice() == mockBluetoothDevice
    }

    public void startConnecting() {
        rxStartConnecting().subscribe({}, {})
    }

    public Observable<RxBleConnection> rxStartConnecting() {
        return rxBleDevice.establishConnection(false)
    }

    public void notifyConnectionWasEstablished() {
        mockConnectorEstablishConnectionPublishSubject.onNext(mockConnection)
    }

    public void dropConnection() {
        mockConnectorEstablishConnectionPublishSubject.onError(new BleGattException(mockBluetoothGatt, BleGattOperationType.CONNECTION_STATE))
    }
}
