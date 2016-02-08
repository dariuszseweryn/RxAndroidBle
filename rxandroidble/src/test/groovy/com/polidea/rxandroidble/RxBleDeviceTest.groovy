package com.polidea.rxandroidble

import android.bluetooth.BluetoothDevice
import com.polidea.rxandroidble.internal.RxBleRadioImpl
import spock.lang.Specification

public class RxBleDeviceTest extends Specification {

    def rxBleRadio = new RxBleRadioImpl()

    def mockBluetoothDevice
    def rxBleDevice

    def setup() {
        mockBluetoothDevice = Mock BluetoothDevice
        rxBleDevice = new RxBleDeviceImpl(mockBluetoothDevice, rxBleRadio)
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

//    TODO: add proper implementation after Dagger integration
//    def "should call connectGatt() on BluetoothDevice eventually"() {
//
//        given:
//        def mockBluetoothDevice = Mock BluetoothDevice
//        def mockContext = Mock Context
//        def rxBleDevice = new RxBleDeviceImpl(mockBluetoothDevice, rxBleRadio)
//        mockBluetoothDevice.connectGatt(context, autoconnect, (BluetoothGattCallback) bluetoothCallback) >> {
//            bluetoothCallback.onStateChanged(null, 0, 2)
//        }
//
//        when:
//
//        def connection = rxBleDevice
//                .establishConnection(mockContext)
//                .toBlocking().first()
//
//        then:
//        connection != null
//        1 * mockBluetoothDevice.connectGatt(mockContext, false, null)
//    }
}
