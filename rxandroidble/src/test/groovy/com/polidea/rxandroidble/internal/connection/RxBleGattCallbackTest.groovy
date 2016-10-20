package com.polidea.rxandroidble.internal.connection

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.polidea.rxandroidble.exceptions.BleDisconnectedException
import org.robospock.RoboSpecification
import rx.internal.schedulers.ImmediateScheduler
import rx.observers.TestSubscriber
import rx.plugins.RxJavaHooks
import spock.lang.Shared
import spock.lang.Unroll

//@Config(manifest = Config.NONE, constants = BuildConfig, sdk = Build.VERSION_CODES.LOLLIPOP)
class RxBleGattCallbackTest extends RoboSpecification {

    def objectUnderTest = new RxBleGattCallback()
    def testSubscriber = new TestSubscriber()
    @Shared def mockBluetoothGatt = Mock BluetoothGatt
    @Shared def mockBluetoothGattCharacteristic = Mock BluetoothGattCharacteristic
    @Shared def mockBluetoothGattDescriptor = Mock BluetoothGattDescriptor

    def setupSpec() {
        RxJavaHooks.reset()
        RxJavaHooks.setOnComputationScheduler({ ImmediateScheduler.INSTANCE })
    }

    def teardownSpec() {
        RxJavaHooks.reset()
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

    def "observeDisconnect() should emit error when .onConnectionStateChange() receives DISCONNECTED state"() {

        given:
        objectUnderTest.observeDisconnect().subscribe(testSubscriber)

        when:
        objectUnderTest.getBluetoothGattCallback().onConnectionStateChange(null, 0, 0)

        then:
        testSubscriber.assertError(BleDisconnectedException)
    }

    def "observeDisconnect() should emit error if .onConnectionStateChange() received DISCONNECTED state"() {

        given:
        objectUnderTest.getBluetoothGattCallback().onConnectionStateChange(null, 0, 0)

        when:
        objectUnderTest.observeDisconnect().subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleDisconnectedException)
    }
}
