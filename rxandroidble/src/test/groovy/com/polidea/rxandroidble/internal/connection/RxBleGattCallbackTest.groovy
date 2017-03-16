package com.polidea.rxandroidble.internal.connection

import static android.bluetooth.BluetoothGatt.GATT_FAILURE
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
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

    // TODO: This test will be deprecated in 2.0.0
    @Unroll
    def "observeDisconnect() should emit error if any of BluetoothGatt.on*() callbacks will receive status != GATT_SUCCESS"() {

        given:
        objectUnderTest.observeDisconnect().subscribe(testSubscriber)

        when:
        callbackCaller.call(objectUnderTest.getBluetoothGattCallback())

        then:
        errorAssertion.call(testSubscriber)

        where:
        callbackCaller << [
                { (it as BluetoothGattCallback).onConnectionStateChange(mockBluetoothGatt, GATT_FAILURE, STATE_CONNECTED) },
                { (it as BluetoothGattCallback).onServicesDiscovered(mockBluetoothGatt, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onCharacteristicRead(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onCharacteristicWrite(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onDescriptorRead(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onDescriptorWrite(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onReadRemoteRssi(mockBluetoothGatt, 1, GATT_FAILURE) }
        ]
        errorAssertion << [
                { (it as TestSubscriber).assertError { it instanceof BleGattException && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattException && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattCharacteristicException && it.characteristic == mockBluetoothGattCharacteristic && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattCharacteristicException && it.characteristic == mockBluetoothGattCharacteristic && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattDescriptorException && it.descriptor == mockBluetoothGattDescriptor && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattDescriptorException && it.descriptor == mockBluetoothGattDescriptor && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattException && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } }
        ]
    }

    // TODO: This test will be deprecated in 2.0.0
    @Unroll
    def "observeDisconnect() should emit error even if any of BluetoothGatt.on*() callbacks received status != GATT_SUCCESS before the subscription"() {

        given:
        callbackCaller.call(objectUnderTest.getBluetoothGattCallback())

        when:
        objectUnderTest.observeDisconnect().subscribe(testSubscriber)

        then:
        errorAssertion.call(testSubscriber)

        where:
        callbackCaller << [
                { (it as BluetoothGattCallback).onConnectionStateChange(mockBluetoothGatt, GATT_FAILURE, STATE_CONNECTED) },
                { (it as BluetoothGattCallback).onServicesDiscovered(mockBluetoothGatt, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onCharacteristicRead(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onCharacteristicWrite(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onDescriptorRead(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onDescriptorWrite(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onReadRemoteRssi(mockBluetoothGatt, 1, GATT_FAILURE) }
        ]
        errorAssertion << [
                { (it as TestSubscriber).assertError { it instanceof BleGattException && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattException && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattCharacteristicException && it.characteristic == mockBluetoothGattCharacteristic && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattCharacteristicException && it.characteristic == mockBluetoothGattCharacteristic && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattDescriptorException && it.descriptor == mockBluetoothGattDescriptor && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattDescriptorException && it.descriptor == mockBluetoothGattDescriptor && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } },
                { (it as TestSubscriber).assertError { it instanceof BleGattException && it.getMacAddress().equals(mockBluetoothDeviceMacAddress) } }
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
        callbackCaller.call(objectUnderTest.getBluetoothGattCallback())

        when:
        observablePicker.call(objectUnderTest).subscribe(testSubscriber)

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

}
