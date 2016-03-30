package com.polidea.rxandroidble.internal

import android.bluetooth.BluetoothDevice
import com.polidea.rxandroidble.FlatRxBleRadio
import com.polidea.rxandroidble.MockRxBleAdapterWrapper
import com.polidea.rxandroidble.internal.util.BleConnectionCompat
import spock.lang.Specification

class RxBleDeviceProviderTest extends Specification {
    def mockRadio = new FlatRxBleRadio()
    def mockAdapterWrapper = Mock MockRxBleAdapterWrapper
    def objectUnderTest = new RxBleDeviceProvider(mockAdapterWrapper, mockRadio, Mock(BleConnectionCompat))

    def setup() {
        mockAdapterWrapper.getRemoteDevice(_) >> {
            String address ->
                def device = Mock(BluetoothDevice)
                device.getAddress() >> address
                device
        }
    }

    def "should return new BleDevice if getBleDevice was called for the first time"() {
        given:
        def macAddress = 'AA:AA:AA:AA:AA:AA'

        when:
        def device = objectUnderTest.getBleDevice(macAddress)

        then:
        device.macAddress == macAddress
    }

    def "should return cached BleDevice for the same mac address"() {
        given:
        def macAddress = 'AA:AA:AA:AA:AA:AA'

        when:
        def device = objectUnderTest.getBleDevice(macAddress)
        def secondDevice = objectUnderTest.getBleDevice(macAddress)

        then:
        device.is secondDevice
    }

    def "should return new BleDevice for the same mac address if reference was garbage collected"() {
        given:
        def macAddress = 'AA:AA:AA:AA:AA:AA'
        def device = objectUnderTest.getBleDevice(macAddress)
        def firstDeviceHashCode = System.identityHashCode(device)

        when:
        device = null
        System.gc()

        then:
        def secondDevice = objectUnderTest.getBleDevice(macAddress)
        firstDeviceHashCode != System.identityHashCode(secondDevice)
    }

    def "should return new BleDevice even though previously there was a call for different mac address"() {
        given:
        def macAddress = 'AA:AA:AA:AA:AA:AA'
        def differentAddress = 'BB:BB:BB:BB:BB:BB'

        when:
        def device = objectUnderTest.getBleDevice(macAddress)
        def secondDevice = objectUnderTest.getBleDevice(differentAddress)

        then:
        device != secondDevice

        and:
        secondDevice.macAddress == differentAddress
    }
}
