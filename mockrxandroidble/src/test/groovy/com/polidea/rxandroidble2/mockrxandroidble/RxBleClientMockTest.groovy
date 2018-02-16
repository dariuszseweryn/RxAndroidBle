package com.polidea.rxandroidble2.mockrxandroidble

import android.os.Build
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.robolectric.annotation.Config
import org.robospock.RoboSpecification

@Config(manifest = Config.NONE, constants = BuildConfig, sdk = Build.VERSION_CODES.LOLLIPOP)
public class RxBleClientMockTest extends RoboSpecification {

    def serviceUUID = UUID.fromString("00001234-0000-0000-8000-000000000000")
    def characteristicUUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
    def characteristicNotifiedUUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
    def characteristicData = "Polidea".getBytes()
    def descriptorUUID = UUID.fromString("00001337-0000-1000-8000-00805f9b34fb")
    def descriptorData = "Config".getBytes()
    def RxBleClient rxBleClient
    def PublishSubject characteristicNotificationSubject = PublishSubject.create()

    def createDevice(deviceName, macAddress, rssi) {
        new RxBleClientMock.DeviceBuilder()
                .deviceMacAddress(macAddress)
                .deviceName(deviceName)
                .scanRecord("ScanRecord".getBytes())
                .rssi(rssi)
                .notificationSource(characteristicNotifiedUUID, characteristicNotificationSubject)
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
        ).build()
    }

    def setup() {
        rxBleClient = new RxBleClientMock.Builder()
                .addDevice(
                createDevice("TestDevice", "AA:BB:CC:DD:EE:FF", 42)
        ).build()
    }

    def "should return filtered BluetoothDevice"() {
        when:
        def testSubscriber = rxBleClient.scanBleDevices(serviceUUID)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getMacAddress() }
                .test()

        then:
        testSubscriber.assertValue("AA:BB:CC:DD:EE:FF")
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

    def "should return the BluetoothDevice mtu"() {
        when:
        def testSubscriber = rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice() }
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(false) }
                .flatMapSingle { rxBleConnection -> rxBleConnection.requestMtu(72) }
                .test()

        then:
        testSubscriber.assertValue(72)
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

    def "should return services list"() {
        when:
        def testSubscriber = rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice() }
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(false) }
                .flatMapSingle { rxBleConnection ->
            rxBleConnection.discoverServices()
                    .map { rxBleDeviceServices -> rxBleDeviceServices.getBluetoothGattServices() }
                    .map { servicesList -> servicesList.size() }
        }
        .test()

        then:
        testSubscriber.assertValue(1)
    }

    def "should return characteristic data"() {
        when:
        def testSubscriber = rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice() }
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(false) }
                .flatMapSingle { rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUUID) }
                .map { data -> new String(data) }
                .test()

        then:
        testSubscriber.assertValue("Polidea")
    }

    def "should return descriptor data"() {
        when:
        def testSubscriber = rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice() }
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(false) }
                .flatMapSingle { rxBleConnection -> rxBleConnection.readDescriptor(serviceUUID, characteristicUUID, descriptorUUID) }
                .map { data -> new String(data) }
                .test()

        then:
        testSubscriber.assertValue("Config")
    }

    def "should return notification data"() {
        given:
        def testSubscriber = rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice() }
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(false) }
                .flatMap { rxBleConnection -> rxBleConnection.setupNotification(characteristicNotifiedUUID) }
                .flatMap({ Observable<byte[]> observable -> observable })
                .map { new String(it) }
                .test()

        when:
        characteristicNotificationSubject.onNext("NotificationData".getBytes())

        then:
        testSubscriber.assertValue("NotificationData")
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
