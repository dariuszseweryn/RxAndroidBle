package com.polidea.rxandroidble2.internal.operations

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble2.internal.connection.BluetoothGattProvider
import com.polidea.rxandroidble2.internal.connection.ConnectionStateChangeListener
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble2.internal.util.BleConnectionCompat
import com.polidea.rxandroidble2.internal.util.MockOperationTimeoutConfiguration
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import spock.lang.Specification

import java.util.concurrent.TimeUnit

public class OperationConnectTest extends Specification {

    BluetoothDevice mockBluetoothDevice = Mock BluetoothDevice
    BluetoothGatt mockGatt = Mock BluetoothGatt
    String mockMacAddress = "test"
    RxBleGattCallback mockCallback
    BleConnectionCompat mockBleConnectionCompat
    MockOperationTimeoutConfiguration timeoutConfiguration
    PublishSubject<RxBleConnection.RxBleConnectionState> onConnectionStateSubject = PublishSubject.create()
    PublishSubject observeDisconnectPublishSubject = PublishSubject.create()
    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface
    ConnectionStateChangeListener mockConnectionStateChangeListener = Mock ConnectionStateChangeListener
    BluetoothGattProvider mockBluetoothGattProvider
    TestScheduler timeoutScheduler
    ConnectOperation objectUnderTest

    def setup() {
        mockBluetoothGattProvider = Mock(BluetoothGattProvider)
        mockCallback = Mock RxBleGattCallback
        mockCallback.getOnConnectionStateChange() >> onConnectionStateSubject
        mockCallback.observeDisconnect() >> observeDisconnectPublishSubject

        timeoutScheduler = new TestScheduler()
        timeoutConfiguration = new MockOperationTimeoutConfiguration(timeoutScheduler)

        mockBleConnectionCompat = Mock(BleConnectionCompat)
        mockBleConnectionCompat.connectGatt(_, _, _) >> mockGatt

        mockGatt.getDevice() >> mockBluetoothDevice
        mockBluetoothDevice.getAddress() >> mockMacAddress

        prepareObjectUnderTest(false)
    }

    def prepareObjectUnderTest(boolean autoConnect) {
        objectUnderTest = new ConnectOperation(mockBluetoothDevice, mockBleConnectionCompat, mockCallback,
                mockBluetoothGattProvider, timeoutConfiguration, autoConnect, mockConnectionStateChangeListener)
    }

    def "asObservable() should not emit onNext before connection is established"() {

        given:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        emitConnectingConnectionState()

        then:
        testSubscriber.assertNoValues()
    }

    def "asObservable() should emit onNext after connection is established"() {

        given:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        emitConnectedConnectionState()

        then:
        testSubscriber.assertValueCount(1)
    }

    def "asObservable() should emit onNext with BluetoothGatt after connection is established"() {

        given:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        emitConnectedConnectionState()

        then:
        testSubscriber.assertAnyOnNext {
            it instanceof BluetoothGatt
        }
    }

    def "should complete after successful connection"() {

        given:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        emitConnectedConnectionState()

        then:
        testSubscriber.assertComplete()
    }

    def "should release QueueReleaseInterface after successful connection"() {

        given:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        emitConnectedConnectionState()

        then:
        (1.._) * mockQueueReleaseInterface.release()
    }

    def "should release QueueReleaseInterface when connection failed"() {

        given:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        emitConnectionError(new Throwable("test"))

        then:
        (1.._) * mockQueueReleaseInterface.release()
    }

    def "should release QueueReleaseInterface when unsubscribed before connection is established"() {

        given:
        def asObservableSubscription = objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        asObservableSubscription.dispose()

        then:
        1 * mockQueueReleaseInterface.release()
    }

    def "should emit BluetoothGattCallbackTimeoutException with a valid mac address on CallbackTimeout"() {

        given:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()
        mockBluetoothGattProvider.getBluetoothGatt() >> mockGatt

        when:
        timeoutScheduler.advanceTimeBy(35, TimeUnit.SECONDS)

        then:
        testSubscriber.assertError {
            it instanceof BleGattCallbackTimeoutException && it.getMacAddress() == mockMacAddress
        }
    }

    def "should call connectionStateChangedAction with CONNECTING when run"() {

        given:
        def observable = objectUnderTest.run(mockQueueReleaseInterface)

        when:
        observable.subscribe()

        then:
        1 * mockConnectionStateChangeListener.onConnectionStateChange(RxBleConnection.RxBleConnectionState.CONNECTING)
    }

    def "should call connectionStateChangedAction with CONNECTED when connected"() {

        given:
        objectUnderTest.run(mockQueueReleaseInterface).subscribe()

        when:
        emitConnectedConnectionState()

        then:
        1 * mockConnectionStateChangeListener.onConnectionStateChange(RxBleConnection.RxBleConnectionState.CONNECTED)
    }

    private emitConnectedConnectionState() {
        mockBluetoothGattProvider.getBluetoothGatt() >> mockGatt
        onConnectionStateSubject.onNext(RxBleConnection.RxBleConnectionState.CONNECTED)
    }

    private emitConnectingConnectionState() {
        onConnectionStateSubject.onNext(RxBleConnection.RxBleConnectionState.CONNECTING)
    }

    private emitConnectionError(Throwable throwable) {
        onConnectionStateSubject.onError(throwable)
    }
}