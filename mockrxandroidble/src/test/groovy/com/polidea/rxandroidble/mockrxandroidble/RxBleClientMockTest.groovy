package com.polidea.rxandroidble.mockrxandroidble

import android.os.Build
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

    def setup() {
        rxBleClient = new RxBleClientMock.Builder()
                .deviceMacAddress("AA:BB:CC:DD:EE:FF")
                .deviceName("TestDevice")
                .scanRecord("ScanRecord".getBytes())
                .rssi(42)
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
        ).build();
    }

    def "should return the BluetoothDevice name"() {
        given:
        def testSubscriber = TestSubscriber.create()

        when:
        rxBleClient.scanBleDevices(null)
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
        rxBleClient.scanBleDevices(null)
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
        rxBleClient.scanBleDevices(null)
                .take(1)
                .map { scanResult -> scanResult.getRssi() }
                .subscribe(testSubscriber)

        then:
        testSubscriber.assertValue(42)
    }

    def "should return services list"() {
        given:
        def testSubscriber = TestSubscriber.create()

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
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(RuntimeEnvironment.application) }
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
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(RuntimeEnvironment.application) }
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
                .flatMap { rxBleDevice -> rxBleDevice.establishConnection(RuntimeEnvironment.application) }
                .flatMap { rxBleConnection -> rxBleConnection.getNotification(characteristicNotifiedUUID) }
                .subscribe { obs -> obs.map { data -> new String(data) } subscribe(testSubscriber) }

        when:
        characteristicNotificationSubject.onNext("NotificationData".getBytes())

        then:
        testSubscriber.assertValue("NotificationData")
    }
}
