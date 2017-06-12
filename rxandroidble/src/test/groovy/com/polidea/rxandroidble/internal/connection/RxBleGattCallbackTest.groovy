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
import org.robospock.RoboSpecification
import rx.internal.schedulers.ImmediateScheduler
import rx.observers.TestSubscriber
import rx.plugins.RxJavaHooks
import spock.lang.Shared
import spock.lang.Unroll

class RxBleGattCallbackTest extends RoboSpecification {

    def objectUnderTest = new RxBleGattCallback(ImmediateScheduler.INSTANCE, Mock(BluetoothGattProvider))
    def testSubscriber = new TestSubscriber()
    @Shared def mockBluetoothGatt = Mock BluetoothGatt
    @Shared def mockBluetoothGattCharacteristic = Mock BluetoothGattCharacteristic
    @Shared def mockBluetoothGattDescriptor = Mock BluetoothGattDescriptor
    @Shared def mockBluetoothDevice = Mock BluetoothDevice
    @Shared def mockBluetoothDeviceMacAddress = "MacAddress"

    def setupSpec() {
        RxJavaHooks.reset()
        RxJavaHooks.setOnComputationScheduler({ ImmediateScheduler.INSTANCE })
        mockBluetoothGatt.getDevice() >> mockBluetoothDevice
        mockBluetoothDevice.getAddress() >> mockBluetoothDeviceMacAddress
    }

    def teardownSpec() {
        RxJavaHooks.reset()
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

    def "observeDisconnect() should emit error when .onConnectionStateChange() receives STATE_DISCONNECTED"() {

        given:
        objectUnderTest.observeDisconnect().subscribe(testSubscriber)

        when:
        objectUnderTest.getBluetoothGattCallback().onConnectionStateChange(mockBluetoothGatt, GATT_SUCCESS, STATE_DISCONNECTED)

        then:
        testSubscriber.assertError(BleDisconnectedException)
    }

    def "observeDisconnect() should emit error even if .onConnectionStateChange() received STATE_DISCONNECTED before the subscription"() {

        given:
        objectUnderTest.getBluetoothGattCallback().onConnectionStateChange(mockBluetoothGatt, GATT_SUCCESS, STATE_DISCONNECTED)

        when:
        objectUnderTest.observeDisconnect().subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleDisconnectedException)
    }

    @Unroll
    def "observeDisconnect() should not emit error if any of BluetoothGatt.on*() [other than onConnectionStateChanged()] callbacks will receive status != GATT_SUCCESS"() {

        given:
        objectUnderTest.observeDisconnect().subscribe(testSubscriber)

        when:
        callbackCaller.call(objectUnderTest.getBluetoothGattCallback())

        then:
        testSubscriber.assertNoErrors()

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
    def "observeDisconnect() should not emit error even if any of BluetoothGatt.on*() [other than onConnectionStateChanged()] callbacks received status != GATT_SUCCESS before the subscription"() {

        given:
        callbackCaller.call(objectUnderTest.getBluetoothGattCallback())

        when:
        objectUnderTest.observeDisconnect().subscribe(testSubscriber)

        then:
        testSubscriber.assertNoErrors()

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

    def "observeDisconnect() should emit error if BluetoothGatt.onConnectionStateChange() callback will receive status != GATT_SUCCESS"() {

        given:
        objectUnderTest.observeDisconnect().subscribe(testSubscriber)

        when:
        objectUnderTest.getBluetoothGattCallback().onConnectionStateChange(mockBluetoothGatt, GATT_FAILURE, STATE_CONNECTED)

        then:
        testSubscriber.assertError { it instanceof BleGattException && it.getMacAddress() == mockBluetoothDeviceMacAddress }
    }

    def "observeDisconnect() should emit error even if BluetoothGatt.onConnectionStateChange() callback received status != GATT_SUCCESS before the subscription"() {

        given:
        objectUnderTest.getBluetoothGattCallback().onConnectionStateChange(mockBluetoothGatt, GATT_FAILURE, STATE_CONNECTED)

        when:
        objectUnderTest.observeDisconnect().subscribe(testSubscriber)

        then:
        testSubscriber.assertError { it instanceof BleGattException && it.getMacAddress() == mockBluetoothDeviceMacAddress }
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
    def "callbacks other than getOnConnectionStateChange() should throw if onConnectionStateChange() received STATE_DISCONNECTED"() {

        given:
        observableGetter.call(objectUnderTest).subscribe(testSubscriber)

        when:
        objectUnderTest.getBluetoothGattCallback().onConnectionStateChange(mockBluetoothGatt, GATT_SUCCESS, STATE_DISCONNECTED)

        then:
        testSubscriber.assertError(BleDisconnectedException)

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
                { (it as TestSubscriber).assertError { it instanceof BleGattException && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattCharacteristicException && it.characteristic == mockBluetoothGattCharacteristic && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattCharacteristicException && it.characteristic == mockBluetoothGattCharacteristic && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattDescriptorException && it.descriptor == mockBluetoothGattDescriptor && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattDescriptorException && it.descriptor == mockBluetoothGattDescriptor && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattException && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } }
        ]
    }

    @Unroll
    def "should transmit error to all callbacks [other than getOnConnectionStateChanged()] when onConnectionStateChanged() will get status != BluetoothGatt.GATT_SUCCESS"() {

        given:
        givenSubscription.call(objectUnderTest).subscribe(testSubscriber)

        when:
        objectUnderTest.getBluetoothGattCallback().onConnectionStateChange(mockBluetoothGatt, GATT_FAILURE, (int) newStateGatt)

        then:
        testSubscriber.assertError(BleGattException.class)

        where:
        newStateGatt        | givenSubscription
        STATE_DISCONNECTED  | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnCharacteristicRead() }
        STATE_DISCONNECTED  | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnCharacteristicWrite() }
        STATE_DISCONNECTED  | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnCharacteristicChanged() }
        STATE_DISCONNECTED  | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnDescriptorRead() }
        STATE_DISCONNECTED  | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnDescriptorWrite() }
        STATE_DISCONNECTED  | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnRssiRead() }
        STATE_DISCONNECTED  | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnServicesDiscovered() }
        STATE_CONNECTING    | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnCharacteristicRead() }
        STATE_CONNECTING    | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnCharacteristicWrite() }
        STATE_CONNECTING    | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnCharacteristicChanged() }
        STATE_CONNECTING    | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnDescriptorRead() }
        STATE_CONNECTING    | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnDescriptorWrite() }
        STATE_CONNECTING    | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnRssiRead() }
        STATE_CONNECTING    | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnServicesDiscovered() }
        STATE_CONNECTED     | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnCharacteristicRead() }
        STATE_CONNECTED     | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnCharacteristicWrite() }
        STATE_CONNECTED     | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnCharacteristicChanged() }
        STATE_CONNECTED     | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnDescriptorRead() }
        STATE_CONNECTED     | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnDescriptorWrite() }
        STATE_CONNECTED     | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnRssiRead() }
        STATE_CONNECTED     | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnServicesDiscovered() }
        STATE_DISCONNECTING | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnCharacteristicRead() }
        STATE_DISCONNECTING | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnCharacteristicWrite() }
        STATE_DISCONNECTING | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnCharacteristicChanged() }
        STATE_DISCONNECTING | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnDescriptorRead() }
        STATE_DISCONNECTING | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnDescriptorWrite() }
        STATE_DISCONNECTING | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnRssiRead() }
        STATE_DISCONNECTING | { RxBleGattCallback objectUnderTest-> objectUnderTest.getOnServicesDiscovered() }
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

    @Unroll
    def "should not transmit error to onConnectionStateChanged if other callbacks will get status != BluetoothGatt.GATT_SUCCESS"() {

        given:
        objectUnderTest.getOnConnectionStateChange().subscribe(testSubscriber)

        when:
        whenAction.call(objectUnderTest.getBluetoothGattCallback(), GATT_FAILURE)

        then:
        testSubscriber.assertNoErrors()
        testSubscriber.assertNoValues()

        where:
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
