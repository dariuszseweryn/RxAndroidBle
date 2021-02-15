package com.polidea.rxandroidble3.scan

import android.os.Build
import android.os.ParcelUuid
import com.polidea.rxandroidble3.BuildConfig
import com.polidea.rxandroidble3.internal.scan.RxBleInternalScanResult
import hkhc.electricspock.ElectricSpecification
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE, constants = BuildConfig, sdk = Build.VERSION_CODES.LOLLIPOP)
public class ScanFilterTest extends ElectricSpecification {

    RxBleInternalScanResult mockInternalScanResult = Mock RxBleInternalScanResult

    ScanRecord mockScanRecord = Mock ScanRecord

    ScanFilter objectUnderTest

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

    def "should not match by service uuid"() {

        given:
        ParcelUuid uuid = ParcelUuid.fromString("00001234-0000-0000-8000-000000000000")
        ParcelUuid uuid2 = ParcelUuid.fromString("00001235-0000-0000-8000-000000000000")
        givenScanRecordWith serviceUuids: [uuid]
        objectUnderTest = new ScanFilter.Builder().setServiceUuid(uuid2).build()

        expect:
        !objectUnderTest.matches(mockInternalScanResult)
    }

    def "should match by service uuid masked"() {

        given:
        ParcelUuid uuid = ParcelUuid.fromString("00001234-0000-0000-8000-000000000000")
        ParcelUuid uuid2 = ParcelUuid.fromString("00001235-0000-0000-8000-000000000000")
        ParcelUuid uuidMask = ParcelUuid.fromString("0000FFF0-0000-0000-F000-000000000000")
        givenScanRecordWith serviceUuids: [uuid]
        objectUnderTest = new ScanFilter.Builder().setServiceUuid(uuid2, uuidMask).build()

        expect:
        objectUnderTest.matches(mockInternalScanResult)
    }

    def "should not match by service uuid masked"() {

        given:
        ParcelUuid uuid = ParcelUuid.fromString("00001234-0000-0000-8000-000000000000")
        ParcelUuid uuid2 = ParcelUuid.fromString("00001235-0000-0000-8000-000000000000")
        ParcelUuid uuidMask = ParcelUuid.fromString("0000FFFF-0000-0000-F000-000000000000")
        givenScanRecordWith serviceUuids: [uuid]
        objectUnderTest = new ScanFilter.Builder().setServiceUuid(uuid2, uuidMask).build()

        expect:
        !objectUnderTest.matches(mockInternalScanResult)
    }

    def "should match by service data"() {

        given:
        ParcelUuid uuid = ParcelUuid.fromString("00001234-0000-0000-8000-000000000000")
        byte[] data = [0x12, 0x34] as byte[]
        givenScanRecordWith serviceData: data
        objectUnderTest = new ScanFilter.Builder().setServiceData(uuid, data).build()

        expect:
        objectUnderTest.matches(mockInternalScanResult)
    }

    def "should not match by service data"() {

        given:
        ParcelUuid uuid = ParcelUuid.fromString("00001234-0000-0000-8000-000000000000")
        byte[] data = [0x12, 0x34] as byte[]
        byte[] data2 = [0x12, 0x56] as byte[]
        givenScanRecordWith serviceData: data
        objectUnderTest = new ScanFilter.Builder().setServiceData(uuid, data2).build()

        expect:
        !objectUnderTest.matches(mockInternalScanResult)
    }

    def "should match by service data masked"() {

        given:
        ParcelUuid uuid = ParcelUuid.fromString("00001234-0000-0000-8000-000000000000")
        byte[] data =  [0x12, 0x34] as byte[]
        byte[] data2 = [0x12, 0x56] as byte[]
        byte[] mask =  [0x12, 0x00] as byte[]
        givenScanRecordWith serviceData: data
        objectUnderTest = new ScanFilter.Builder().setServiceData(uuid, data2, mask).build()

        expect:
        objectUnderTest.matches(mockInternalScanResult)
    }

    def "should not match by service data masked"() {

        given:
        ParcelUuid uuid = ParcelUuid.fromString("00001234-0000-0000-8000-000000000000")
        byte[] data =  [0x12, 0x34] as byte[]
        byte[] data2 = [0x12, 0x56] as byte[]
        byte[] mask =  [0x12, 0xFF] as byte[]
        givenScanRecordWith serviceData: data
        objectUnderTest = new ScanFilter.Builder().setServiceData(uuid, data2, mask).build()

        expect:
        !objectUnderTest.matches(mockInternalScanResult)
    }

    def "should match by manufacturer data"() {

        given:
        ParcelUuid uuid = ParcelUuid.fromString("00001234-0000-0000-8000-000000000000")
        byte[] data = [0x12, 0x34] as byte[]
        givenScanRecordWith manufacturerSpecificData: data
        objectUnderTest = new ScanFilter.Builder().setManufacturerData(0, data).build()

        expect:
        objectUnderTest.matches(mockInternalScanResult)
    }

    def "should not match by manufacturer data"() {

        given:
        ParcelUuid uuid = ParcelUuid.fromString("00001234-0000-0000-8000-000000000000")
        byte[] data = [0x12, 0x34] as byte[]
        byte[] data2 = [0x12, 0x56] as byte[]
        givenScanRecordWith manufacturerSpecificData: data
        objectUnderTest = new ScanFilter.Builder().setManufacturerData(0, data2).build()

        expect:
        !objectUnderTest.matches(mockInternalScanResult)
    }

    def "should match by manufacturer data masked"() {

        given:
        ParcelUuid uuid = ParcelUuid.fromString("00001234-0000-0000-8000-000000000000")
        byte[] data =  [0x12, 0x34] as byte[]
        byte[] data2 = [0x12, 0x56] as byte[]
        byte[] mask =  [0x12, 0x00] as byte[]
        givenScanRecordWith manufacturerSpecificData: data
        objectUnderTest = new ScanFilter.Builder().setManufacturerData(0, data2, mask).build()

        expect:
        objectUnderTest.matches(mockInternalScanResult)
    }

    def "should not match by manufacturer data masked"() {

        given:
        ParcelUuid uuid = ParcelUuid.fromString("00001234-0000-0000-8000-000000000000")
        byte[] data =  [0x12, 0x34] as byte[]
        byte[] data2 = [0x12, 0x56] as byte[]
        byte[] mask =  [0x12, 0xFF] as byte[]
        givenScanRecordWith manufacturerSpecificData: data
        objectUnderTest = new ScanFilter.Builder().setManufacturerData(0, data2, mask).build()

        expect:
        !objectUnderTest.matches(mockInternalScanResult)
    }

    private void givenScanRecordWith(Map scanRecordMap) {
        mockInternalScanResult.getScanRecord() >> mockScanRecord
        mockScanRecord.getDeviceName() >> (scanRecordMap['deviceName'] ?: null)
        mockScanRecord.getServiceUuids() >> (scanRecordMap['serviceUuids'] ?: null)
        mockScanRecord.getServiceData(_) >> (scanRecordMap['serviceData'] ?: null)
        mockScanRecord.getManufacturerSpecificData(_) >> (scanRecordMap['manufacturerSpecificData'] ?: null)
    }
}
