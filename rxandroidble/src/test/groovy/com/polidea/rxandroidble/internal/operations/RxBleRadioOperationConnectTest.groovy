package com.polidea.rxandroidble.internal.operations

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble.internal.util.BleConnectionCompat

import java.util.concurrent.Semaphore
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject
import spock.lang.Specification

public class RxBleRadioOperationConnectTest extends Specification {

    Context mockContext = Mock Context
    BleConnectionCompat connectionCompat = new BleConnectionCompat(mockContext)
    BluetoothDevice mockBluetoothDevice = Mock BluetoothDevice
    BluetoothGatt mockGatt = Mock BluetoothGatt
    RxBleGattCallback mockCallback = Mock RxBleGattCallback
    TestSubscriber<BluetoothGatt> testSubscriber = new TestSubscriber()
    TestSubscriber<BluetoothGatt> getGattSubscriber = new TestSubscriber()
    PublishSubject<RxBleConnection.RxBleConnectionState> onConnectionStateSubject = PublishSubject.create()
    PublishSubject<BluetoothGatt> bluetoothGattPublishSubject = PublishSubject.create()
    Semaphore mockSemaphore = Mock Semaphore
    RxBleRadioOperationConnect objectUnderTest

    def setup() {
        mockCallback.getOnConnectionStateChange() >> onConnectionStateSubject
        mockCallback.getBluetoothGatt() >> bluetoothGattPublishSubject
        prepareObjectUnderTest(false)
    }

    def prepareObjectUnderTest(boolean autoConnect) {
        objectUnderTest = new RxBleRadioOperationConnect(mockBluetoothDevice, mockCallback, connectionCompat, autoConnect)
        objectUnderTest.setRadioBlockingSemaphore(mockSemaphore)
        objectUnderTest.asObservable().subscribe(testSubscriber)
        objectUnderTest.getBluetoothGatt().subscribe(getGattSubscriber)
    }

    def "asObservable() should not emit onNext before connection is established"() {

        given:
        objectUnderTest.run()

        when:
        emitConnectingConnectionState()

        then:
        testSubscriber.assertNoValues()
    }

    def "asObservable() should emit onNext after connection is established"() {

        given:
        objectUnderTest.run()

        when:
        emitConnectedConnectionState()

        then:
        testSubscriber.assertValueCount(1)
    }

    def "asObservable() should emit onNext with BluetoothGatt after connection is established"() {

        given:
        objectUnderTest.run()

        when:
        emitConnectedConnectionState()

        then:
        testSubscriber.assertAnyOnNext {
            it instanceof BluetoothGatt
        }
    }

    def "getBluetoothGatt() should emit onNext each time RxBleCallback emits getBluetoothGatt()"() {

        given:
        objectUnderTest.run()
        emitConnectingConnectionState()
        emitConnectedConnectionState()

        expect:
        getGattSubscriber.assertValueCount(3) // + 1 after returning from run()
    }

    def "getBluetoothGatt() should not emit onError when connection error comes"() {

        given:
        objectUnderTest.run()

        when:
        emitConnectionError(new Throwable("test"))

        then:
        getGattSubscriber.assertNoErrors()
    }

    def "getBluetoothGatt() should emit onNext even when connection error comes"() {

        given:
        objectUnderTest.run()

        when:
        emitConnectionError(new Throwable("test"))

        then:
        getGattSubscriber.assertValueCount(2) // + 1 after returning from run()
    }

    def "getBluetoothGatt() should complete RxBleGattCallback.getBluetoothGatt() completes"() {

        given:
        objectUnderTest.run()

        when:
        bluetoothGattPublishSubject.onCompleted()

        then:
        getGattSubscriber.assertCompleted()
    }

    def "getBluetoothGatt() should complete when error comes"() {

        given:
        objectUnderTest.run()

        when:
        emitConnectionError(new Throwable("test"))

        then:
        getGattSubscriber.assertCompleted()
    }

    def "should release Semaphore after successful connection"() {

        given:
        objectUnderTest.run()

        when:
        emitConnectedConnectionState()

        then:
        1 * mockSemaphore.release()
    }

    def "should release Semaphore when connection failed"() {

        given:
        objectUnderTest.run()

        when:
        emitConnectionError(new Throwable("test"))

        then:
        1 * mockSemaphore.release()
    }

    private emitConnectedConnectionState() {
        bluetoothGattPublishSubject.onNext(mockGatt)
        onConnectionStateSubject.onNext(RxBleConnection.RxBleConnectionState.CONNECTED)
    }

    private emitConnectingConnectionState() {
        bluetoothGattPublishSubject.onNext(mockGatt)
        onConnectionStateSubject.onNext(RxBleConnection.RxBleConnectionState.CONNECTING)
    }

    private emitConnectionError(Throwable throwable) {
        bluetoothGattPublishSubject.onNext(mockGatt)
        onConnectionStateSubject.onError(throwable)
    }
}