package com.polidea.rxandroidble.mockrxandroidble

import android.content.Context
import spock.lang.Specification

public class RxBleClientMockTest extends Specification {

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
                .rssi(42)
                .rxBleDeviceServices(
                new RxBleClientMock.ServicesBuilder()
                        .addService(
                            serviceUUID,
                            new RxBleClientMock.CharacteristicsBuilder(characteristicUUID, characteristicData)
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
        expect:
        rxBleClient.scanBleDevices(null)
                .take(1)
                .map {scanResult -> scanResult.getBleDevice() }
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(Mock(Context)) }
                .flatMap { rxBleConnection ->
                        rxBleConnection
                        .discoverServices()
                        .map { rxBleDeviceServices -> rxBleDeviceServices.getBluetoothGattServices() }
                        .map { servicesList -> servicesList.size() }
                }
                .subscribe { size -> size == 1 }
    }

    def "should return characteristic data"() {
        expect:
        rxBleClient.scanBleDevices(null)
                .take(1)
                .map {scanResult -> scanResult.getBleDevice() }
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(Mock(Context)) }
                .flatMap { rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUUID) }
                .subscribe { characteristicData -> characteristicData == "Polidea" }
    }
}
