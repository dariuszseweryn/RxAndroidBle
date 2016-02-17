package com.polidea.rxandroidble.mockrxandroidble

import android.os.Build
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robospock.RoboSpecification

@Config(manifest = Config.NONE, constants = BuildConfig, sdk = Build.VERSION_CODES.LOLLIPOP)
public class RxBleClientMockTest extends RoboSpecification {

    def serviceUUID = UUID.fromString("00001234-0000-0000-8000-000000000000")
    def characteristicUUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
    def characteristicData = "Polidea".getBytes()
    def descriptorUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    def descriptorData = "Config".getBytes();
    def rxBleClient

    def setup() {
        rxBleClient = new RxBleClientMock.Builder()
                .deviceMacAddress("AA:BB:CC:DD:EE:FF")
                .deviceName("TestDevice")
                .scanRecord("ScanRecord".getBytes())
                .rssi(42)
                .addService(
                serviceUUID,
                new RxBleClientMock.CharacteristicsBuilder()
                        .addCharacteristic(
                        characteristicUUID,
                        characteristicData,
                        new RxBleClientMock.DescriptorsBuilder()
                                .addDescriptor(descriptorUUID, descriptorData)
                                .build()
                ).build()
        ).build();
    }

    def "should return the BluetoothDevice name"() {
        given:
        def deviceName = ""

        when:
        rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice() }
                .subscribe { bleDevice -> deviceName = bleDevice.getName() }

        then:
        deviceName == "TestDevice"
    }

    def "should return the BluetoothDevice address"() {
        given:
        def macAddress = ""

        when:
        rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice() }
                .subscribe { bleDevice -> macAddress = bleDevice.getMacAddress() }

        then:
        macAddress == "AA:BB:CC:DD:EE:FF"
    }

    def "should return the BluetoothDevice rssi"() {
        given:
        def rssi = -1

        when:
        rxBleClient.scanBleDevices(null)
                .take(1)
                .subscribe { scanResult -> rssi = scanResult.getRssi() }

        then:
        rssi == 42
    }

    def "should return services list"() {
        given:
        def size = -1;

        when:
        rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice() }
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(RuntimeEnvironment.application) }
                .flatMap { rxBleConnection ->
            rxBleConnection
                    .discoverServices()
                    .map { rxBleDeviceServices -> rxBleDeviceServices.getBluetoothGattServices() }
                    .map { servicesList -> servicesList.size() }
        }
        .subscribe { listSize -> size = listSize }

        then:
        size == 1;
    }

    def "should return characteristic data"() {
        given:
        def data = ""

        when:
        rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice() }
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(RuntimeEnvironment.application) }
                .flatMap { rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUUID) }
                .subscribe { characteristicData -> data = characteristicData }

        then:
        new String(data) == "Polidea"
    }

    def "should return descriptor data"() {
        given:
        def data = ""

        when:
        rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice() }
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(RuntimeEnvironment.application) }
                .flatMap { rxBleConnection -> rxBleConnection.readDescriptor(serviceUUID, characteristicUUID, descriptorUUID) }
                .subscribe { descriptorData -> data = descriptorData }

        then:
        new String(data) == "Config"
    }
}
