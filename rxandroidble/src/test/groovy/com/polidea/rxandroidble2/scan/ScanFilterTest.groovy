package com.polidea.rxandroidble2.scan

import android.bluetooth.BluetoothDevice
import com.polidea.rxandroidble2.internal.scan.RxBleInternalScanResult
import spock.lang.Specification

class ScanFilterTest extends Specification {

    RxBleInternalScanResult mockInternalScanResult = Mock RxBleInternalScanResult

    ScanRecord mockScanRecord = Mock ScanRecord

    ScanFilter objectUnderTest

    def setup() {
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
        mockInternalScanResult.getDeviceName() >> name
        objectUnderTest = new ScanFilter.Builder().setDeviceName(name).build()

        expect:
        objectUnderTest.matches(mockInternalScanResult)
    }

    def "should not match by device name if the name is present in BluetoothDevice and ScanRecord but neither match"() {

        given:
        String name = "xxx"
        String name2 = "yyy"
        mockInternalScanResult.getDeviceName() >> name
        givenScanRecordWith deviceName: name
        objectUnderTest = new ScanFilter.Builder().setDeviceName(name2).build()

        expect:
        !objectUnderTest.matches(mockInternalScanResult)
    }

    def "should not match by device name if the name is not present in BluetoothDevice nor ScanRecord"() {

        given:
        String name = "xxx"
        objectUnderTest = new ScanFilter.Builder().setDeviceName(name).build()

        expect:
        !objectUnderTest.matches(mockInternalScanResult)
    }

    def "should match by service uuid"() {

        given:
        ParcelUuid uuid = ParcelUuid.fromString("00001234-0000-0000-8000-000000000000")
        givenScanRecordWith serviceUuids: [uuid]
        objectUnderTest = new ScanFilter.Builder().setServiceUuid(uuid).build()

        expect:
        objectUnderTest.matches(mockInternalScanResult)
    }

    private void givenScanRecordWith(Map scanRecordMap) {
        mockInternalScanResult.getScanRecord() >> mockScanRecord
        mockScanRecord.getDeviceName() >> (scanRecordMap['deviceName'] ?: null)
        mockScanRecord.getServiceUuids() >> (scanRecordMap['serviceUuids'] ?: null)
        mockScanRecord.getServiceData(_) >> (scanRecordMap['serviceData'] ?: null)
        mockScanRecord.getManufacturerSpecificData(_) >> (scanRecordMap['manufacturerSpecificData'] ?: null)
    }
}
