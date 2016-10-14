package com.polidea.rxandroidble.mockrxandroidble

import android.os.Build
import com.polidea.rxandroidble.RxBleConnection
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robospock.RoboSpecification
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject

@Config(manifest = Config.NONE, constants = BuildConfig, sdk = Build.VERSION_CODES.LOLLIPOP)
public class RxBleClientMockTest extends RoboSpecification {

    def serviceUUID = UUID.fromString("00001234-0000-0000-8000-000000000000")
    def characteristicUUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
    def characteristicNotifiedUUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
    def characteristicData = "Polidea".getBytes()
    def descriptorUUID = UUID.fromString("00001337-0000-1000-8000-00805f9b34fb");
    def descriptorData = "Config".getBytes();
    def rxBleClient
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
        ).build();
    }

    def "should return filtered BluetoothDevice"() {
        given:
        def testSubscriber = TestSubscriber.create()

        when:
        rxBleClient.scanBleDevices(serviceUUID)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getMacAddress() }
                .subscribe(testSubscriber)

        then:
        testSubscriber.assertValue("AA:BB:CC:DD:EE:FF")
    }

    def "should return the BluetoothDevice name"() {
        given:
        def testSubscriber = TestSubscriber.create()

        when:
        rxBleClient.scanBleDevices()
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getName() }
                .subscribe(testSubscriber)

        then:
        testSubscriber.assertValue("TestDevice")
    }

    def "should return the BluetoothDevice address"() {
        given:
        def testSubscriber = TestSubscriber.create()

        when:
        rxBleClient.scanBleDevices()
                .take(1)
                .map { scanResult -> scanResult.getBleDevice().getMacAddress() }
                .subscribe(testSubscriber)

        then:
        testSubscriber.assertValue("AA:BB:CC:DD:EE:FF")
    }

    def "should return the BluetoothDevice rssi"() {
        given:
        def testSubscriber = TestSubscriber.create()

        when:
        rxBleClient.scanBleDevices()
                .take(1)
                .map { scanResult -> scanResult.getRssi() }
                .subscribe(testSubscriber)

        then:
        testSubscriber.assertValue(42)
    }

    def "should return the BluetoothDevice mtu"() {
        given:
        def testSubscriber = TestSubscriber.create()

        when:
        rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice() }
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(RuntimeEnvironment.application, false) }
                .flatMap { rxBleConnection ->
            rxBleConnection
                    .requestMtu(72)
        }
        .subscribe(testSubscriber)

        then:
        testSubscriber.assertValue(72)
    }

    def "should return BluetoothDevices that were added on the fly"() {
        given:
        def testNameSubscriber = TestSubscriber.create()
        def testAddressSubscriber = TestSubscriber.create()
        def testRssiSubscriber = TestSubscriber.create()
        rxBleClient.simulateDeviceDiscovery(createDevice("SecondDevice", "AA:BB:CC:DD:EE:00", 17))

        when:
        def scanObservable = rxBleClient.scanBleDevices()
        scanObservable.map { scanResult -> scanResult.getBleDevice().getName() }.subscribe(testNameSubscriber)
        scanObservable.map { scanResult -> scanResult.getBleDevice().getMacAddress() }.subscribe(testAddressSubscriber)
        scanObservable.map { scanResult -> scanResult.getRssi() }.subscribe(testRssiSubscriber)

        then:
        testNameSubscriber.assertValues("TestDevice", "SecondDevice")
        testAddressSubscriber.assertValues("AA:BB:CC:DD:EE:FF", "AA:BB:CC:DD:EE:00")
        testRssiSubscriber.assertValues(42, 17)
    }

    def "should return services list"() {
        given:
        def testSubscriber = TestSubscriber.create()

        when:
        rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice() }
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(RuntimeEnvironment.application, false) }
                .flatMap { rxBleConnection ->
            rxBleConnection
                    .discoverServices()
                    .map { rxBleDeviceServices -> rxBleDeviceServices.getBluetoothGattServices() }
                    .map { servicesList -> servicesList.size() }
        }
        .subscribe(testSubscriber)

        then:
        testSubscriber.assertValue(1)
    }

    def "should return characteristic data"() {
        given:
        def testSubscriber = TestSubscriber.create()

        when:
        rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice() }
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(RuntimeEnvironment.application, false) }
                .flatMap { rxBleConnection -> rxBleConnection.readCharacteristic(characteristicUUID) }
                .map { data -> new String(data) }
                .subscribe(testSubscriber)

        then:
        testSubscriber.assertValue("Polidea")
    }

    def "should return descriptor data"() {
        given:
        def testSubscriber = TestSubscriber.create()

        when:
        rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice() }
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(RuntimeEnvironment.application, false) }
                .flatMap { rxBleConnection -> rxBleConnection.readDescriptor(serviceUUID, characteristicUUID, descriptorUUID) }
                .map { data -> new String(data) }
                .subscribe(testSubscriber)

        then:
        testSubscriber.assertValue("Config")
    }

    def "should return notification data"() {
        given:
        def testSubscriber = TestSubscriber.create()
        rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getBleDevice() }
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(RuntimeEnvironment.application, false) }
                .flatMap { rxBleConnection -> rxBleConnection.setupNotification(characteristicNotifiedUUID) }
                .subscribe { obs -> obs.map { data -> new String(data) } subscribe(testSubscriber) }

        when:
        characteristicNotificationSubject.onNext("NotificationData".getBytes())

        then:
        testSubscriber.assertValue("NotificationData")
    }

    def "should emit correct connection state values when connected"() {
        given:
        def testSubscriber = TestSubscriber.create()
        def device = rxBleClient.getBleDevice("AA:BB:CC:DD:EE:FF")
        device.observeConnectionStateChanges().subscribe(testSubscriber);

        when:
        device.establishConnection(RuntimeEnvironment.application, false).subscribe {}

        then:
        testSubscriber.assertValues(
                RxBleConnection.RxBleConnectionState.DISCONNECTED,
                RxBleConnection.RxBleConnectionState.CONNECTING,
                RxBleConnection.RxBleConnectionState.CONNECTED)
    }

    def "should emit correct connection state values when disconnected"() {
        given:
        def testSubscriber = TestSubscriber.create()
        def device = rxBleClient.getBleDevice("AA:BB:CC:DD:EE:FF")
        device.observeConnectionStateChanges().subscribe(testSubscriber);
        def subscription = device.establishConnection(RuntimeEnvironment.application, false).subscribe {}

        when:
        subscription.unsubscribe()

        then:
        testSubscriber.assertValues(
                RxBleConnection.RxBleConnectionState.DISCONNECTED,
                RxBleConnection.RxBleConnectionState.CONNECTING,
                RxBleConnection.RxBleConnectionState.CONNECTED,
                RxBleConnection.RxBleConnectionState.DISCONNECTED)
    }
}
