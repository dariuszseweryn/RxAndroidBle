package com.polidea.rxandroidble.internal.connection

import static android.bluetooth.BluetoothGatt.GATT_FAILURE
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTING
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.DISCONNECTED

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.polidea.rxandroidble.exceptions.BleDisconnectedException
import com.polidea.rxandroidble.exceptions.BleGattCharacteristicException
import com.polidea.rxandroidble.exceptions.BleGattDescriptorException
import com.polidea.rxandroidble.exceptions.BleGattException
import com.polidea.rxandroidble.exceptions.BleGattOperationType
import org.robospock.RoboSpecification
import rx.internal.schedulers.ImmediateScheduler
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject
import spock.lang.Shared
import spock.lang.Unroll

class RxBleGattCallbackTest extends RoboSpecification {

    DisconnectionRouter mockDisconnectionRouter
    PublishSubject mockDisconnectionSubject
    RxBleGattCallback objectUnderTest
    def testSubscriber = new TestSubscriber()
    @Shared def mockBluetoothGatt = Mock BluetoothGatt
    @Shared def mockBluetoothGattCharacteristic = Mock BluetoothGattCharacteristic
    @Shared def mockBluetoothGattDescriptor = Mock BluetoothGattDescriptor
    @Shared def mockBluetoothDevice = Mock BluetoothDevice
    @Shared def mockBluetoothDeviceMacAddress = "MacAddress"

    def setupSpec() {
        mockBluetoothGatt.getDevice() >> mockBluetoothDevice
        mockBluetoothDevice.getAddress() >> mockBluetoothDeviceMacAddress
    }

    def setup() {
        mockDisconnectionRouter = Mock DisconnectionRouter
        mockDisconnectionSubject = PublishSubject.create()
        mockDisconnectionRouter.asObservable() >> mockDisconnectionSubject
        objectUnderTest = new RxBleGattCallback(ImmediateScheduler.INSTANCE, Mock(BluetoothGattProvider), mockDisconnectionRouter, new NativeCallbackDispatcher())
    }

    def "sanity check"() {

        expect:
        GATT_SUCCESS != GATT_FAILURE
    }

    @Unroll
    def "should relay BluetoothGattCallback callbacks to appropriate Observables"() {

        given:
        observableGetter.call(objectUnderTest).subscribe(testSubscriber)

        when:
        callbackCaller.call(objectUnderTest.getBluetoothGattCallback())

        then:
        testSubscriber.assertValueCount(1)

        where:
        observableGetter << [
                { return (it as RxBleGattCallback).getOnConnectionStateChange() },
                { return (it as RxBleGattCallback).getOnServicesDiscovered() },
                { return (it as RxBleGattCallback).getOnCharacteristicRead() },
                { return (it as RxBleGattCallback).getOnCharacteristicWrite() },
                { return (it as RxBleGattCallback).getOnCharacteristicChanged() },
                { return (it as RxBleGattCallback).getOnDescriptorRead() },
                { return (it as RxBleGattCallback).getOnDescriptorWrite() },
                { return (it as RxBleGattCallback).getOnRssiRead() }
        ]
        callbackCaller << [
                { (it as BluetoothGattCallback).onConnectionStateChange(mockBluetoothGatt, GATT_SUCCESS, STATE_CONNECTED) },
                { (it as BluetoothGattCallback).onServicesDiscovered(mockBluetoothGatt, GATT_SUCCESS) },
                { (it as BluetoothGattCallback).onCharacteristicRead(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_SUCCESS) },
                { (it as BluetoothGattCallback).onCharacteristicWrite(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_SUCCESS) },
                { (it as BluetoothGattCallback).onCharacteristicChanged(mockBluetoothGatt, mockBluetoothGattCharacteristic) },
                { (it as BluetoothGattCallback).onDescriptorRead(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_SUCCESS) },
                { (it as BluetoothGattCallback).onDescriptorWrite(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_SUCCESS) },
                { (it as BluetoothGattCallback).onReadRemoteRssi(mockBluetoothGatt, 1, GATT_SUCCESS) }
        ]
    }

    def "observeDisconnect() should emit error when DisconnectionRouter.asObservable() emits error"() {

        given:
        def testException = new RuntimeException("test")
        objectUnderTest.observeDisconnect().subscribe(testSubscriber)

        when:
        mockDisconnectionSubject.onError(testException)

        then:
        testSubscriber.assertError(testException)
    }

    @Unroll
    def "should call DisconnectionRouter.onDisconnectedException() when .onConnectionStateChange() callback will receive STATE_DISCONNECTED/STATE_DISCONNECTING regardless of status"() {

        when:
        objectUnderTest.getBluetoothGattCallback().onConnectionStateChange(mockBluetoothGatt, status, state)

        then:
        1 * mockDisconnectionRouter.onDisconnectedException({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockBluetoothDeviceMacAddress })

        where:
        [state, status] << [[STATE_DISCONNECTED, STATE_DISCONNECTING], [GATT_SUCCESS, GATT_FAILURE]].combinations()
    }

    @Unroll
    def "should call DisconnectionRouter.onGattConnectionStateException() when .onConnectionStateChange() callback will receive STATE_CONNECTED/STATE_CONNECTING with status != GATT_SUCCESS "() {

        when:
        objectUnderTest.getBluetoothGattCallback().onConnectionStateChange(mockBluetoothGatt, GATT_FAILURE, state)

        then:
        1 * mockDisconnectionRouter.onGattConnectionStateException({ BleGattException e ->
            e.macAddress == mockBluetoothDeviceMacAddress &&
                    e.status == GATT_FAILURE &&
                    e.bleGattOperationType == BleGattOperationType.CONNECTION_STATE
        })

        where:
        state << [STATE_CONNECTED, STATE_CONNECTING]
    }

    @Unroll
    def "observeDisconnect() should not call DisconnectionRouter.route() if any of BluetoothGatt.on*() [other than onConnectionStateChanged()] callbacks will receive status != GATT_SUCCESS"() {

        when:
        callbackCaller.call(objectUnderTest.getBluetoothGattCallback())

        then:
        0 * mockDisconnectionRouter.onDisconnectedException(_)

        and:
        0 * mockDisconnectionRouter.onGattConnectionStateException(_)

        where:
        callbackCaller << [
                { (it as BluetoothGattCallback).onServicesDiscovered(mockBluetoothGatt, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onCharacteristicRead(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onCharacteristicWrite(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onDescriptorRead(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onDescriptorWrite(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onReadRemoteRssi(mockBluetoothGatt, 1, GATT_FAILURE) }
        ]
    }

    @Unroll
    def "getOnConnectionStateChange() should not throw if onConnectionStateChange() received STATE_DISCONNECTED"() {

        given:
        objectUnderTest.getOnConnectionStateChange().subscribe(testSubscriber)

        when:
        objectUnderTest.getBluetoothGattCallback().onConnectionStateChange(mockBluetoothGatt, status, STATE_DISCONNECTED)

        then:
        testSubscriber.assertNoErrors()
        testSubscriber.assertValue(DISCONNECTED)

        where:
        status << [
                GATT_SUCCESS,
                GATT_FAILURE
        ]
    }

    @Unroll
    def "callbacks other than getOnConnectionStateChange() should throw if DisconnectionRouter.asObservable() emits an exception"() {

        given:
        def testException = new RuntimeException("test")
        observableGetter.call(objectUnderTest).subscribe(testSubscriber)

        when:
        mockDisconnectionSubject.onError(testException)

        then:
        testSubscriber.assertError(testException)

        where:
        observableGetter << [
                { return (it as RxBleGattCallback).getOnServicesDiscovered() },
                { return (it as RxBleGattCallback).getOnCharacteristicRead() },
                { return (it as RxBleGattCallback).getOnCharacteristicWrite() },
                { return (it as RxBleGattCallback).getOnCharacteristicChanged() },
                { return (it as RxBleGattCallback).getOnDescriptorRead() },
                { return (it as RxBleGattCallback).getOnDescriptorWrite() },
                { return (it as RxBleGattCallback).getOnRssiRead() }
        ]
    }

    @Unroll
    def "callbacks should emit error if their respective BluetoothGatt.on*() callbacks received status != GATT_SUCCESS"() {

        given:
        observablePicker.call(objectUnderTest).subscribe(testSubscriber)

        when:
        callbackCaller.call(objectUnderTest.getBluetoothGattCallback())

        then:
        errorAssertion.call(testSubscriber)

        where:
        observablePicker << [
                { (it as RxBleGattCallback).getOnServicesDiscovered() },
                { (it as RxBleGattCallback).getOnCharacteristicRead() },
                { (it as RxBleGattCallback).getOnCharacteristicWrite() },
                { (it as RxBleGattCallback).getOnDescriptorRead() },
                { (it as RxBleGattCallback).getOnDescriptorWrite() },
                { (it as RxBleGattCallback).getOnRssiRead() }
        ]
        callbackCaller << [
                { (it as BluetoothGattCallback).onServicesDiscovered(mockBluetoothGatt, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onCharacteristicRead(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onCharacteristicWrite(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onDescriptorRead(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onDescriptorWrite(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onReadRemoteRssi(mockBluetoothGatt, 1, GATT_FAILURE) }
        ]
        errorAssertion << [
                { (it as TestSubscriber).assertError { it instanceof BleGattException && it.getMacAddress() == mockBluetoothDeviceMacAddress } },
                { (it as TestSubscriber).assertError { it instanceof BleGattCharacteristicException && it.characteristic == mockBluetoothGattCharacteristic && it.getMacAddress() == mockBluetoothDeviceMacAddress } },
                { (it as TestSubscriber).assertError { it instanceof BleGattCharacteristicException && it.characteristic == mockBluetoothGattCharacteristic && it.getMacAddress() == mockBluetoothDeviceMacAddress } },
                { (it as TestSubscriber).assertError { it instanceof BleGattDescriptorException && it.descriptor == mockBluetoothGattDescriptor && it.getMacAddress() == mockBluetoothDeviceMacAddress } },
                { (it as TestSubscriber).assertError { it instanceof BleGattDescriptorException && it.descriptor == mockBluetoothGattDescriptor && it.getMacAddress() == mockBluetoothDeviceMacAddress } },
                { (it as TestSubscriber).assertError { it instanceof BleGattException && it.getMacAddress() == mockBluetoothDeviceMacAddress } }
        ]
    }

    @Unroll
    def "should transmit error on proper callback when status != BluetoothGatt.GATT_SUCCESS, subsequent calls to callbacks will work normally"() {

        given:
        def secondTestSubscriber = new TestSubscriber()
        givenSubscription.call(objectUnderTest, testSubscriber)

        when:
        whenAction.call(objectUnderTest.getBluetoothGattCallback(), GATT_FAILURE)

        then:
        testSubscriber.assertError({ it instanceof BleGattException && ((BleGattException) it).status == GATT_FAILURE })

        when:
        givenSubscription.call(objectUnderTest, secondTestSubscriber)
        whenAction.call(objectUnderTest.getBluetoothGattCallback(), GATT_SUCCESS)

        then:
        secondTestSubscriber.assertValueCount(1)

        where:
        givenSubscription << [
                { RxBleGattCallback objectUnderTest, TestSubscriber testSubscriber -> objectUnderTest.getOnCharacteristicRead().subscribe(testSubscriber) },
                { RxBleGattCallback objectUnderTest, TestSubscriber testSubscriber -> objectUnderTest.getOnCharacteristicWrite().subscribe(testSubscriber) },
                { RxBleGattCallback objectUnderTest, TestSubscriber testSubscriber -> objectUnderTest.getOnDescriptorRead().subscribe(testSubscriber) },
                { RxBleGattCallback objectUnderTest, TestSubscriber testSubscriber -> objectUnderTest.getOnDescriptorWrite().subscribe(testSubscriber) },
                { RxBleGattCallback objectUnderTest, TestSubscriber testSubscriber -> objectUnderTest.getOnRssiRead().subscribe(testSubscriber) },
                { RxBleGattCallback objectUnderTest, TestSubscriber testSubscriber -> objectUnderTest.getOnServicesDiscovered().subscribe(testSubscriber) }
        ]
        whenAction << [
                { BluetoothGattCallback callback, int status -> callback.onCharacteristicRead(Mock(BluetoothGatt), Mock(BluetoothGattCharacteristic), status) },
                { BluetoothGattCallback callback, int status -> callback.onCharacteristicWrite(Mock(BluetoothGatt), Mock(BluetoothGattCharacteristic), status) },
                { BluetoothGattCallback callback, int status -> callback.onDescriptorRead(Mock(BluetoothGatt), Mock(BluetoothGattDescriptor), status) },
                { BluetoothGattCallback callback, int status -> callback.onDescriptorWrite(Mock(BluetoothGatt), Mock(BluetoothGattDescriptor), status) },
                { BluetoothGattCallback callback, int status -> callback.onReadRemoteRssi(Mock(BluetoothGatt), 0, status) },
                { BluetoothGattCallback callback, int status -> callback.onServicesDiscovered(Mock(BluetoothGatt), status) }
        ]
    }
}
