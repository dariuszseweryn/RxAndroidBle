package com.polidea.rxandroidble2.mockrxandroidble

import android.os.Build
import android.os.ParcelUuid
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanSettings;
import hkhc.electricspock.ElectricSpecification
import io.reactivex.rxjava3.subjects.PublishSubject
import org.robolectric.annotation.Config
import com.polidea.rxandroidble2.BuildConfig

@Config(manifest = Config.NONE, constants = BuildConfig, sdk = Build.VERSION_CODES.LOLLIPOP)
public class RxBleClientMockTest extends ElectricSpecification {

    def serviceUUID = UUID.fromString("00001234-0000-0000-8000-000000000000")
    def serviceUUID2 = UUID.fromString("00001235-0000-0000-8000-000000000000")
    def characteristicUUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
    def characteristicUUIDNoCallback = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fc")
    def characteristicData = "Polidea".getBytes()
    def descriptorUUID = UUID.fromString("00001337-0000-1000-8000-00805f9b34fb")
    def descriptorData = "Config".getBytes()
    RxBleClient rxBleClient

    def createDevice(deviceName, macAddress, rssi) {
        new RxBleDeviceMock.Builder()
                .deviceMacAddress(macAddress)
                .deviceName(deviceName)
                .scanRecord(
                    new RxBleScanRecordMock.Builder()
                        .setAdvertiseFlags(1)
                        .addServiceUuid(new ParcelUuid(serviceUUID))
                        .addServiceUuid(new ParcelUuid(serviceUUID2))
                        .addServiceSolicitionUuid(new ParcelUuid(serviceUUID))
                        .addManufacturerSpecificData(0x2211, [0x33, 0x44] as byte[])
                        .addServiceData(new ParcelUuid(serviceUUID), [0x11, 0x22] as byte[])
                        .setTxPowerLevel(12)
                        .setDeviceName("TestDeviceAdv")
                        .build()
                )
                .connection(new RxBleConnectionMock.Builder()
                        .rssi(rssi)
                        .addService(serviceUUID,
                                new RxBleClientMock.CharacteristicsBuilder()
                                        .addCharacteristic(
                                                characteristicUUID,
                                                characteristicData,
                                                new RxBleClientMock.DescriptorsBuilder()
                                                        .addDescriptor(descriptorUUID, descriptorData)
                                                        .build()
                                        ).addCharacteristic(
                                                characteristicUUIDNoCallback,
                                                characteristicData,
                                                new RxBleClientMock.DescriptorsBuilder()
                                                        .addDescriptor(descriptorUUID, descriptorData)
                                                        .build()
                                        ).build()
                        )
                        .build()
                ).build()
    }

    def setup() {
        rxBleClient = new RxBleClientMock.Builder()
                .addDevice(
                createDevice("TestDevice", "AA:BB:CC:DD:EE:FF", 42)
        ).build()
    }

    def "should return filtered BluetoothDevice filtered on service UUID"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUUID)).build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getMacAddress() }
                .test()

        then:
        testSubscriber.assertValue("AA:BB:CC:DD:EE:FF")
    }

    def "should return filtered BluetoothDevice filtered on service UUID only in scan record"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUUID2)).build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getMacAddress() }
                .test()

        then:
        testSubscriber.assertValue("AA:BB:CC:DD:EE:FF")
    }

    def "should not return filtered BluetoothDevice filtered on invalid service UUID"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setServiceUuid(
                new ParcelUuid(UUID.fromString("00001236-0000-0000-8000-000000000000"))
        ).build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .test()

        then:
        testSubscriber.assertEmpty()
    }

    def "should return filtered BluetoothDevice filtered on masked service UUID"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder()
                .setServiceUuid(
                        new ParcelUuid(UUID.fromString("00001230-0000-0000-8000-000000000000")),
                        new ParcelUuid(UUID.fromString("0000FFF0-0000-0000-8000-000000000000"))
                )
                .build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getMacAddress() }
                .test()

        then:
        testSubscriber.assertValue("AA:BB:CC:DD:EE:FF")
    }

    def "should not return filtered BluetoothDevice filtered on invalid masked service UUID"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder()
                .setServiceUuid(
                        new ParcelUuid(UUID.fromString("00001200-0000-0000-8000-000000000000")),
                        new ParcelUuid(UUID.fromString("0000FFF0-0000-0000-8000-000000000000"))
                )
                .build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .test()

        then:
        testSubscriber.assertEmpty()
    }

    def "should return filtered BluetoothDevice filtered on service solicitation UUID"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setServiceSolicitationUuid(new ParcelUuid(serviceUUID)).build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getMacAddress() }
                .test()

        then:
        testSubscriber.assertValue("AA:BB:CC:DD:EE:FF")
    }

    def "should not return filtered BluetoothDevice filtered on invalid service solicitation UUID"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setServiceSolicitationUuid(
                new ParcelUuid(UUID.fromString("00001236-0000-0000-8000-000000000000"))
        ).build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .test()

        then:
        testSubscriber.assertEmpty()
    }

    def "should return filtered BluetoothDevice filtered on masked service solicitation UUID"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder()
                .setServiceSolicitationUuid(
                        new ParcelUuid(UUID.fromString("00001230-0000-0000-8000-000000000000")),
                        new ParcelUuid(UUID.fromString("0000FFF0-0000-0000-8000-000000000000"))
                )
                .build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getMacAddress() }
                .test()

        then:
        testSubscriber.assertValue("AA:BB:CC:DD:EE:FF")
    }

    def "should not return filtered BluetoothDevice filtered on invalid masked service solicitation UUID"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder()
                .setServiceSolicitationUuid(
                        new ParcelUuid(UUID.fromString("00001200-0000-0000-8000-000000000000")),
                        new ParcelUuid(UUID.fromString("0000FFF0-0000-0000-8000-000000000000"))
                )
                .build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .test()

        then:
        testSubscriber.assertEmpty()
    }

    def "should return filtered BluetoothDevice filtered on manufacturer data"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setManufacturerData(0x2211, [0x33, 0x44] as byte[]).build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getMacAddress() }
                .test()

        then:
        testSubscriber.assertValue("AA:BB:CC:DD:EE:FF")
    }

    def "should not return filtered BluetoothDevice filtered on invalid manufacturer id"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setManufacturerData(0x2212, [0x33, 0x44] as byte[]).build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .test()

        then:
        testSubscriber.assertEmpty()
    }

    def "should return filtered BluetoothDevice filtered on invalid manufacturer data"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setManufacturerData(0x2211, [0x33, 0x45] as byte[]).build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .test()

        then:
        testSubscriber.assertEmpty()
    }

    def "should return filtered BluetoothDevice filtered on masked manufacturer data"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setManufacturerData(0x2211, [0x30, 0x40] as byte[], [0xF0, 0xF0] as byte[]).build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getMacAddress() }
                .test()

        then:
        testSubscriber.assertValue("AA:BB:CC:DD:EE:FF")
    }

    def "should not return filtered BluetoothDevice filtered on masked manufacturer data"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setManufacturerData(0x2211, [0x30, 0x40] as byte[], [0xFF, 0xFF] as byte[]).build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .test()

        then:
        testSubscriber.assertEmpty()
    }

    def "should return filtered BluetoothDevice filtered on service data"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setServiceData(new ParcelUuid(serviceUUID), [0x11, 0x22] as byte[]).build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getMacAddress() }
                .test()

        then:
        testSubscriber.assertValue("AA:BB:CC:DD:EE:FF")
    }

    def "should not return filtered BluetoothDevice filtered on service data"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setServiceData(new ParcelUuid(serviceUUID), [0x11, 0x33] as byte[]).build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .test()

        then:
        testSubscriber.assertEmpty()
    }

    def "should return filtered BluetoothDevice filtered on masked service data"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setServiceData(new ParcelUuid(serviceUUID), [0x10, 0x20] as byte[], [0xF0, 0xF0] as byte[]).build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getMacAddress() }
                .test()

        then:
        testSubscriber.assertValue("AA:BB:CC:DD:EE:FF")
    }

    def "should not return filtered BluetoothDevice filtered on masked service data"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setServiceData(new ParcelUuid(serviceUUID), [0x10, 0x20] as byte[], [0xFF, 0xFF] as byte[]).build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .test()

        then:
        testSubscriber.assertEmpty()
    }

    def "should return filtered BluetoothDevice filtered on device name"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setDeviceName("TestDeviceAdv").build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getMacAddress() }
                .test()

        then:
        testSubscriber.assertValue("AA:BB:CC:DD:EE:FF")
    }

    def "should not return filtered BluetoothDevice filtered on device name"() {
        when:
        def scanSettings = new ScanSettings.Builder().build()
        def scanFilter = new ScanFilter.Builder().setDeviceName("TestDeviceAdvv").build()
        def testSubscriber = rxBleClient.scanBleDevices(scanSettings, scanFilter)
                .test()

        then:
        testSubscriber.assertEmpty()
    }

    def "should return the BluetoothDevice name"() {
        when:
        def testSubscriber = rxBleClient.scanBleDevices()
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getName() }
                .test()

        then:
        testSubscriber.assertValue("TestDevice")
    }

    def "should return the BluetoothDevice address"() {
        when:
        def testSubscriber = rxBleClient.scanBleDevices()
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getMacAddress() }
                .test()

        then:
        testSubscriber.assertValue("AA:BB:CC:DD:EE:FF")
    }

    def "should return the BluetoothDevice rssi"() {
        when:
        def testSubscriber = rxBleClient.scanBleDevices()
                .take(1)
                .map { scanResult -> scanResult.getRssi() }
                .test()

        then:
        testSubscriber.assertValue(42)
    }

    def "should return BluetoothDevices that were added on the fly"() {
        given:
        def discoverableDevicesSubject = PublishSubject.create()
        def dynRxBleClient = new RxBleClientMock.Builder()
                .setDeviceDiscoveryObservable(discoverableDevicesSubject)
                .build()
        discoverableDevicesSubject.onNext(createDevice("TestDevice", "AA:BB:CC:DD:EE:FF", 42))
        discoverableDevicesSubject.onNext(createDevice("SecondDevice", "AA:BB:CC:DD:EE:00", 17))

        when:
        def scanObservable = dynRxBleClient.scanBleDevices()
        def testNameSubscriber = scanObservable.map { scanResult -> scanResult.getBleDevice().getName() }.test()
        def testAddressSubscriber = scanObservable.map { scanResult -> scanResult.getBleDevice().getMacAddress() }.test()
        def testRssiSubscriber = scanObservable.map { scanResult -> scanResult.getRssi() }.test()

        then:
        testNameSubscriber.assertValues("TestDevice", "SecondDevice")
        testAddressSubscriber.assertValues("AA:BB:CC:DD:EE:FF", "AA:BB:CC:DD:EE:00")
        testRssiSubscriber.assertValues(42, 17)
    }

    def "should emit correct connection state values when connected"() {
        given:
        def device = rxBleClient.getBleDevice("AA:BB:CC:DD:EE:FF")
        def testSubscriber = device.observeConnectionStateChanges().test()

        when:
        device.establishConnection(false).subscribe {}

        then:
        testSubscriber.assertValues(
                RxBleConnection.RxBleConnectionState.DISCONNECTED,
                RxBleConnection.RxBleConnectionState.CONNECTING,
                RxBleConnection.RxBleConnectionState.CONNECTED)
    }

    def "should emit correct connection state values when disconnected"() {
        given:
        def device = rxBleClient.getBleDevice("AA:BB:CC:DD:EE:FF")
        def testSubscriber = device.observeConnectionStateChanges().test()
        def subscription = device.establishConnection(false).test()

        when:
        subscription.dispose()

        then:
        testSubscriber.assertValues(
                RxBleConnection.RxBleConnectionState.DISCONNECTED,
                RxBleConnection.RxBleConnectionState.CONNECTING,
                RxBleConnection.RxBleConnectionState.CONNECTED,
                RxBleConnection.RxBleConnectionState.DISCONNECTED)
    }
}
