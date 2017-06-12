package com.polidea.rxandroidble.internal.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import spock.lang.Specification

class RxBleAdapterWrapperTest extends Specification {

    def mockAdapter = Mock BluetoothAdapter
    def objectUnderTest = new RxBleAdapterWrapper(mockAdapter)

    def "should check if adapter is provided"() {

        when:
        def objectUnderTest = new RxBleAdapterWrapper(adapterInstance)

        then:
        objectUnderTest.hasBluetoothAdapter() == expectedResult

        where:
        adapterInstance        | expectedResult
        null                   | false
        Mock(BluetoothAdapter) | true
    }

    def "should pass through getRemoteDevice()"() {
        given:
        def macAddress = 'AA:AA:AA:AA:AA:AA'
        def mock = Mock BluetoothDevice

        when:
        def remoteDevice = objectUnderTest.getRemoteDevice(macAddress)


        then:
        1 * mockAdapter.getRemoteDevice(macAddress) >> mock

        and:
        remoteDevice.is mock

    }

    def "should pass through startLeScan()"() {
        given:
        def callback = Mock BluetoothAdapter.LeScanCallback

        when:
        def startLeScan = objectUnderTest.startLegacyLeScan(callback)

        then:
        1 * mockAdapter.startLeScan(callback) >> startResult

        and:
        startLeScan == startResult

        where:
        startResult | _
        true        | _
        false       | _
    }

    def "should pass through stopScan()"() {
        given:
        def callback = Mock BluetoothAdapter.LeScanCallback

        when:
        objectUnderTest.stopLegacyLeScan(callback)

        then:
        1 * mockAdapter.stopLeScan(callback)
    }

    def "should check if bluetooth is enabled"() {
        when:
        def isEnabled = objectUnderTest.isBluetoothEnabled()

        then:
        1 * mockAdapter.isEnabled() >> isEnabledInAdapter

        and:
        isEnabled == isEnabledInAdapter

        where:
        isEnabledInAdapter | _
        true               | _
        false              | _
    }

    def "should not throw NullPointerException if BluetoothAdapter.getBluetoothLeScanner() will return null in .stopScan(ScanCallback)"() {

        given:
        def callback = Mock ScanCallback
        mockAdapter.getBluetoothLeScanner() >> null

        when:
        objectUnderTest.stopLeScan(callback)

        then:
        notThrown(NullPointerException)
    }
}
