package com.polidea.rxandroidble.scan

import android.bluetooth.BluetoothDevice
import com.polidea.rxandroidble.internal.scan.RxBleInternalScanResult
import spock.lang.Specification

class ScanFilterTest extends Specification {

    RxBleInternalScanResult mockInternalScanResult = Mock RxBleInternalScanResult

    BluetoothDevice mockBluetoothDevice = Mock BluetoothDevice

    ScanRecord mockScanRecord = Mock ScanRecord

    ScanFilter objectUnderTest

    def setup() {
        mockInternalScanResult.getBluetoothDevice() >> mockBluetoothDevice
        mockInternalScanResult.getScanRecord() >> mockScanRecord
    }

    def "should match by device name if the name is present in ScanRecord"() {

        given:
        String name = "xxx"
        givenScanRecordWith deviceName: name
        objectUnderTest = new ScanFilter.Builder().setDeviceName(name).build()

        expect:
        objectUnderTest.matches(mockInternalScanResult)
    }

    def "should match by device name if the name is present in BluetoothDevice"() {

        given:
        String name = "xxx"
        mockBluetoothDevice.getName() >> name
        objectUnderTest = new ScanFilter.Builder().setDeviceName(name).build()

        expect:
        objectUnderTest.matches(mockInternalScanResult)
    }

    def "should not match by device name if the name is not present in BluetoothDevice nor ScanRecord"() {

        given:
        String name = "xxx"
        objectUnderTest = new ScanFilter.Builder().setDeviceName(name).build()

        expect:
        !objectUnderTest.matches(mockInternalScanResult)
    }

    private void givenScanRecordWith(Map scanRecordMap) {
        mockScanRecord.getDeviceName() >> (scanRecordMap['deviceName'] ?: null)
        mockScanRecord.getServiceUuids() >> (scanRecordMap['serviceUuids'] ?: null)
        mockScanRecord.getServiceData(_) >> (scanRecordMap['serviceData'] ?: null)
        mockScanRecord.getManufacturerSpecificData(_) >> (scanRecordMap['manufacturerSpecificData'] ?: null)
    }
}
