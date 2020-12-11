package com.polidea.rxandroidble2.mockrxandroidble.internal

import android.os.Build
import android.os.ParcelUuid
import com.polidea.rxandroidble2.mockrxandroidble.RxBleScanRecordMock
import hkhc.electricspock.ElectricSpecification
import org.robolectric.annotation.Config
import com.polidea.rxandroidble2.BuildConfig

@Config(manifest = Config.NONE, constants = BuildConfig, sdk = Build.VERSION_CODES.LOLLIPOP)
public class ScanRecordDataConstructorTest extends ElectricSpecification {

    def setup() {

    }

    def "should construct scan record with advertising flags"() {
        given:
        def scanRecord = new RxBleScanRecordMock.Builder()
            .setAdvertiseFlags(0x12)
            .build()

        when:
        def data = new ScanRecordDataConstructor(false).constructBytesFromScanRecord(scanRecord)

        then:
        data == [0x02, 0x01, 0x12] as byte[]
    }

    def "should construct scan record with device name"() {
        given:
        def name = "DeviceName"
        def nameBytes = name.getBytes("UTF-8")
        def scanRecord = new RxBleScanRecordMock.Builder()
                .setDeviceName(name)
                .build()

        when:
        def data = new ScanRecordDataConstructor(false).constructBytesFromScanRecord(scanRecord)

        then:
        data == [nameBytes.length + 1, 0x09] + (nameBytes as List) as byte[]
    }

    def "should construct scan record with short device name"() {
        given:
        def name = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1"
        def nameBytes = name.getBytes("UTF-8")
        def scanRecord = new RxBleScanRecordMock.Builder()
                .setDeviceName(name)
                .build()

        when:
        def data = new ScanRecordDataConstructor(false).constructBytesFromScanRecord(scanRecord)

        then:
        data == [26 + 1, 0x08] + (nameBytes as List).subList(0, 26) as byte[]
    }

    def "should construct scan record with manufacturers"() {
        given:
        def scanRecord = new RxBleScanRecordMock.Builder()
                .addManufacturerSpecificData(0x1234, [ 0x34, 0x56 ] as byte[])
                .addManufacturerSpecificData(0x3456, [ 0x78, 0x90 ] as byte[])
                .build()

        when:
        def data = new ScanRecordDataConstructor(false).constructBytesFromScanRecord(scanRecord)

        then:
        data == [
                0x05, 0xFF, 0x34, 0x12, 0x34, 0x56,
                0x05, 0xFF, 0x56, 0x34, 0x78, 0x90
                ] as byte[]
    }

    def "should construct scan record with tx power"() {
        given:
        def scanRecord = new RxBleScanRecordMock.Builder()
                .setTxPowerLevel(70)
                .build()

        when:
        def data = new ScanRecordDataConstructor(false).constructBytesFromScanRecord(scanRecord)

        then:
        data == [0x02, 0x0A, 70] as byte[]
    }

    def "should construct scan record with 16 bit service uuids"() {
        given:
        def scanRecord = new RxBleScanRecordMock.Builder()
                .addServiceUuid(ParcelUuid.fromString("00001234-0000-1000-8000-00805F9B34FB"))
                .addServiceUuid(ParcelUuid.fromString("00005678-0000-1000-8000-00805F9B34FB"))
                .build()

        when:
        def data = new ScanRecordDataConstructor(false).constructBytesFromScanRecord(scanRecord)

        then:
        data == [
                1 + 2 + 2, 0x03,
                0x34, 0x12,
                0x78, 0x56
        ] as byte[]
    }

    def "should construct scan record with 32 bit service uuids"() {
        given:
        def scanRecord = new RxBleScanRecordMock.Builder()
                .addServiceUuid(ParcelUuid.fromString("12345678-0000-1000-8000-00805F9B34FB"))
                .addServiceUuid(ParcelUuid.fromString("87654321-0000-1000-8000-00805F9B34FB"))
                .build()

        when:
        def data = new ScanRecordDataConstructor(false).constructBytesFromScanRecord(scanRecord)

        then:
        data == [
                1 + 4 + 4, 0x05,
                0x78, 0x56, 0x34, 0x12,
                0x21, 0x43, 0x65, 0x87
        ] as byte[]
    }

    def "should construct scan record with 128 bit service uuids"() {
        given:
        def scanRecord = new RxBleScanRecordMock.Builder()
                .addServiceUuid(ParcelUuid.fromString("12345678-0000-2000-8000-00805F9B34FB")) // difference in MSB
                .addServiceUuid(ParcelUuid.fromString("87654321-0000-1000-7000-00805F9B34FB")) // difference in LSB
                .build()

        when:
        def data = new ScanRecordDataConstructor(false).constructBytesFromScanRecord(scanRecord)

        then:
        data == [
                1 + 16 + 16, 0x07,
                0xFB, 0x34, 0x9B, 0x5F, 0x80, 0x00, 0x00, 0x80, 0x00, 0x20, 0x00, 0x00, 0x78, 0x56, 0x34, 0x12,
                0xFB, 0x34, 0x9B, 0x5F, 0x80, 0x00, 0x00, 0x70, 0x00, 0x10, 0x00, 0x00, 0x21, 0x43, 0x65, 0x87
        ] as byte[]
    }

    def "should construct scan record with service data"() {
        given:
        def scanRecord = new RxBleScanRecordMock.Builder()
                .addServiceData(ParcelUuid.fromString("00005678-0000-1000-8000-00805F9B34FB"), [0x12, 0x34] as byte[]) // 16 bit
                .addServiceData(ParcelUuid.fromString("12345678-0000-1000-8000-00805F9B34FB"), [0x56, 0x78] as byte[]) // 32 bit
                .addServiceData(ParcelUuid.fromString("12345678-0000-2000-8000-00805F9B34FB"), [0x90, 0x12] as byte[]) // 128 bit
                .build()

        when:
        def data = new ScanRecordDataConstructor(false).constructBytesFromScanRecord(scanRecord)

        then:
        data == [
                1 + 2 + 2, 0x16, // length, type
                0x78, 0x56, // uuid
                0x12, 0x34, // data
                1 + 4 + 2, 0x20, // length, type
                0x78, 0x56, 0x34, 0x12, // uuid
                0x56, 0x78, // data
                1 + 16 + 2, 0x21, // length, type
                0xFB, 0x34, 0x9B, 0x5F, 0x80, 0x00, 0x00, 0x80, 0x00, 0x20, 0x00, 0x00, 0x78, 0x56, 0x34, 0x12, // uuid
                0x90, 0x12 // data
        ] as byte[]
    }

    def "should construct scan record with legacy data length"() {
        given:
        def name = "DeviceName1234"
        def nameBytes = name.getBytes("UTF-8")
        def scanRecord = new RxBleScanRecordMock.Builder()
                .setDeviceName(name)
                .addServiceUuid(ParcelUuid.fromString("12345678-0000-2000-8000-00805F9B34FB")) // 128 bit
                .build()

        when:
        def data = new ScanRecordDataConstructor(true).constructBytesFromScanRecord(scanRecord)

        then:
        data == [nameBytes.length + 1, 0x09] + (nameBytes as List) + [
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // padding
                0x00, 0x00, 0x00, 0x00, 0x00, // padding
                1 + 16, 0x07, // (128 bit uuids) length, type
                0xFB, 0x34, 0x9B, 0x5F, 0x80, 0x00, 0x00, 0x80, 0x00, 0x20, 0x00, 0x00, 0x78, 0x56, 0x34, 0x12,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        ] as byte[]
    }

    def "should not construct scan record with legacy data length when too much data"() {
        given:
        def name = "DeviceName1234"
        def nameBytes = name.getBytes("UTF-8")
        def scanRecord = new RxBleScanRecordMock.Builder()
                .setDeviceName(name)
                .addServiceUuid(ParcelUuid.fromString("12345678-0000-2000-8000-00805F9B34FB")) // 128 bit
                .addServiceUuid(ParcelUuid.fromString("12345679-0000-2000-8000-00805F9B34FB")) // 128 bit
                .build()

        when:
        def data = new ScanRecordDataConstructor(true).constructBytesFromScanRecord(scanRecord)

        then:
        IllegalArgumentException ex = thrown()
        ex.message == "Could not fit scan response inside legacy data packets"
    }

}
