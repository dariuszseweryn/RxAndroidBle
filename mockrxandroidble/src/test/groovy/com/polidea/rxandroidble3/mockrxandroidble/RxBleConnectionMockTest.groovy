package com.polidea.rxandroidble3.mockrxandroidble

import android.os.Build
import com.polidea.rxandroidble3.RxBleClient
import com.polidea.rxandroidble3.exceptions.BleDisconnectedException
import com.polidea.rxandroidble3.exceptions.BleGattCharacteristicException
import com.polidea.rxandroidble3.exceptions.BleGattDescriptorException
import com.polidea.rxandroidble3.mockrxandroidble.callbacks.results.RxBleGattReadResultMock
import com.polidea.rxandroidble3.mockrxandroidble.callbacks.results.RxBleGattWriteResultMock
import hkhc.electricspock.ElectricSpecification
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import org.robolectric.annotation.Config
import com.polidea.rxandroidble3.BuildConfig

@Config(manifest = Config.NONE, constants = BuildConfig, sdk = Build.VERSION_CODES.LOLLIPOP)
public class RxBleConnectionMockTest extends ElectricSpecification {

    def serviceUUID = UUID.fromString("00001234-0000-0000-8000-000000000000")
    def characteristicUUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
    def characteristicUUIDNoCallback = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fc")
    def characteristicNotifiedUUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
    def characteristicValue = "Polidea"
    def characteristicData = characteristicValue.getBytes()
    def descriptorUUID = UUID.fromString("00001337-0000-1000-8000-00805f9b34fb")
    def descriptorValue = "Config"
    def descriptorData = descriptorValue.getBytes()
    RxBleClient rxBleClient
    RxBleDeviceMock rxBleDeviceMock
    RxBleConnectionMock rxBleConnectionMock
    PublishSubject characteristicNotificationSubject = PublishSubject.create()

    PublishSubject<RxBleGattReadResultMock> characteristicReadSubject = PublishSubject.create()
    PublishSubject<RxBleGattWriteResultMock> characteristicWriteSubject = PublishSubject.create()
    PublishSubject<RxBleGattReadResultMock> descriptorReadSubject = PublishSubject.create()
    PublishSubject<RxBleGattWriteResultMock> descriptorWriteSubject = PublishSubject.create()

    def nextCharacteristicReadWillResult(closure) {
        characteristicReadSubject.take(1).subscribe(closure)
    }

    def nextCharacteristicWriteWillResult(closure) {
        characteristicWriteSubject.take(1).subscribe(closure)
    }

    def nextDescriptorReadWillResult(closure) {
        descriptorReadSubject.take(1).subscribe(closure)
    }

    def nextDescriptorWriteWillResult(closure) {
        descriptorWriteSubject.take(1).subscribe(closure)
    }

    def createConnection(rssi) {
        new RxBleConnectionMock.Builder()
                .rssi(rssi)
                .notificationSource(characteristicNotifiedUUID, characteristicNotificationSubject)
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
                .characteristicReadCallback(characteristicUUID, { device, characteristic, result ->
                    characteristicReadSubject.onNext(result)
                })
                .characteristicWriteCallback(characteristicUUID, { device, characteristic, bytes, result ->
                    characteristicWriteSubject.onNext(result)
                })
                .descriptorReadCallback(characteristicUUID, descriptorUUID, { device, descriptor, result ->
                    descriptorReadSubject.onNext(result)
                })
                .descriptorWriteCallback(characteristicUUID, descriptorUUID, { device, descriptor, bytes, result ->
                    descriptorWriteSubject.onNext(result)
                })
                .build()
    }

    def createDevice(macAddress, rssi) {
        def connection = createConnection(rssi)
        new Tuple2(new RxBleDeviceMock.Builder()
                .deviceMacAddress(macAddress)
                .scanRecord(
                    new RxBleScanRecordMock.Builder()
                        .setAdvertiseFlags(1)
                        .build()
                )
                .connection(connection).build(),
            connection)
    }

    def setup() {
        def (RxBleDeviceMock device, RxBleConnectionMock connection) = createDevice("AA:BB:CC:DD:EE:FF", 42)
        rxBleClient = new RxBleClientMock.Builder().addDevice(device).build()
        rxBleDeviceMock = device
        rxBleConnectionMock = connection
    }

    def "should return the BluetoothDevice mtu"() {
        when:
        def testSubscriber = rxBleConnectionMock
                .requestMtu(72)
                .test()

        then:
        testSubscriber.assertValue(72)
    }

    def "should return services list"() {
        when:
        def testSubscriber = rxBleConnectionMock
                .discoverServices()
                .map { rxBleDeviceServices -> rxBleDeviceServices.getBluetoothGattServices() }
                .map { servicesList -> servicesList.size() }
                .test()

        then:
        testSubscriber.assertValue(1)
    }

    def "should return characteristic data"() {
        when:
        def testSubscriber = rxBleConnectionMock
                .readCharacteristic(characteristicUUIDNoCallback)
                .map { data -> new String(data) }
                .test()


        then:
        testSubscriber.assertValue(characteristicValue)
    }

    def "should return characteristic data via callback"() {
        given:
        nextCharacteristicReadWillResult { result -> result.success(characteristicData) }
        
        when:
        def testSubscriber = rxBleConnectionMock
                .readCharacteristic(characteristicUUID)
                .map { data -> new String(data) }
                .test()

        then:
        testSubscriber.assertValue(characteristicValue)
    }

    def "should throw characteristic read error via callback"() {
        given:
        nextCharacteristicReadWillResult { result -> result.failure(0x80) }

        when:
        def testSubscriber = rxBleConnectionMock
                .readCharacteristic(characteristicUUID)
                .map { data -> new String(data) }
                .test()

        then:
        testSubscriber.assertError { throwable ->
            return BleGattCharacteristicException.class.isInstance(throwable) && ((BleGattCharacteristicException) throwable).getStatus() == 0x80
        }
    }

    def "should throw characteristic read disconnection via callback"() {
        given:
        nextCharacteristicReadWillResult { result -> result.disconnect(0x80) }

        when:
        def testSubscriber = rxBleConnectionMock
                .readCharacteristic(characteristicUUID)
                .map { data -> new String(data) }
                .test()

        then:
        testSubscriber.assertError { throwable ->
            return BleDisconnectedException.class.isInstance(throwable) && ((BleDisconnectedException) throwable).state == 0x80
        }
    }

    def "should write characteristic data via callback"() {
        given:
        nextCharacteristicWriteWillResult { result -> result.success() }

        when:
        def testSubscriber = rxBleConnectionMock
                .writeCharacteristic(characteristicUUID, characteristicData)
                .map { data -> new String(data) }
                .test()

        then:
        testSubscriber.assertValue(characteristicValue)
    }

    def "should fail to write characteristic data via callback"() {
        given:
        nextCharacteristicWriteWillResult { result -> result.failure(0x80) }

        when:
        def testSubscriber = rxBleConnectionMock
                .writeCharacteristic(characteristicUUID, characteristicData)
                .map { data -> new String(data) }
                .test()

        then:
        testSubscriber.assertError({ throwable ->
            return BleGattCharacteristicException.class.isInstance(throwable) && ((BleGattCharacteristicException) throwable).getStatus() == 0x80
        })
    }

    def "should fail to write characteristic data due to disconnection via callback"() {
        given:
        nextCharacteristicWriteWillResult { result -> result.disconnect(0x80) }

        when:
        def testSubscriber = rxBleConnectionMock
                .writeCharacteristic(characteristicUUID, characteristicData)
                .map { data -> new String(data) }
                .test()

        then:
        testSubscriber.assertError({ throwable ->
            return BleDisconnectedException.class.isInstance(throwable) && ((BleDisconnectedException) throwable).state == 0x80
        })
    }

    def "should return descriptor data"() {
        when:
        def testSubscriber = rxBleConnectionMock
                .readDescriptor(serviceUUID, characteristicUUIDNoCallback, descriptorUUID)
                .map { data -> new String(data) }
                .test()

        then:
        testSubscriber.assertValue(descriptorValue)
    }

    def "should return descriptor data via callback"() {
        given:
        nextDescriptorReadWillResult { result -> result.success(descriptorData) }

        when:
        def testSubscriber = rxBleConnectionMock
                .readDescriptor(serviceUUID, characteristicUUID, descriptorUUID)
                .map { data -> new String(data) }
                .test()

        then:
        testSubscriber.assertValue(descriptorValue)
    }

    def "should throw descriptor read error via callback"() {
        given:
        nextDescriptorReadWillResult { result -> result.failure(0x80) }

        when:
        def testSubscriber = rxBleConnectionMock
                .readDescriptor(serviceUUID, characteristicUUID, descriptorUUID)
                .map { data -> new String(data) }
                .test()

        then:
        testSubscriber.assertError({ throwable ->
            return BleGattDescriptorException.class.isInstance(throwable) && ((BleGattDescriptorException) throwable).getStatus() == 0x80
        })
    }

    def "should throw descriptor read disconnection error via callback"() {
        given:
        nextDescriptorReadWillResult { result -> result.disconnect(0x80) }

        when:
        def testSubscriber = rxBleConnectionMock
                .readDescriptor(serviceUUID, characteristicUUID, descriptorUUID)
                .map { data -> new String(data) }
                .test()

        then:
        testSubscriber.assertError({ throwable ->
            return BleDisconnectedException.class.isInstance(throwable) && ((BleDisconnectedException) throwable).state == 0x80
        })
    }

    def "should write descriptor data via callback"() {
        given:
        nextDescriptorWriteWillResult { result -> result.success() }

        when:
        def testSubscriber = rxBleConnectionMock
                .writeDescriptor(serviceUUID, characteristicUUID, descriptorUUID, descriptorData)
                .test()

        then:
        testSubscriber.assertComplete();
    }

    def "should fail to write descriptor data via callback"() {
        given:
        nextDescriptorWriteWillResult { result -> result.failure(0x80) }

        when:
        def testSubscriber = rxBleConnectionMock
                .writeDescriptor(serviceUUID, characteristicUUID, descriptorUUID, descriptorData)
                .test()

        then:
        testSubscriber.assertError({ throwable ->
            return BleGattDescriptorException.class.isInstance(throwable) && ((BleGattDescriptorException) throwable).getStatus() == 0x80
        })
    }

    def "should fail to write descriptor data via callback due to disconnection"() {
        given:
        nextDescriptorWriteWillResult { result -> result.disconnect(0x80) }

        when:
        def testSubscriber = rxBleConnectionMock
                .writeDescriptor(serviceUUID, characteristicUUID, descriptorUUID, descriptorData)
                .test()

        then:
        testSubscriber.assertError({ throwable ->
            return BleDisconnectedException.class.isInstance(throwable) && ((BleDisconnectedException) throwable).state == 0x80
        })
    }

    def "should return notification data"() {
        given:
        def testSubscriber = rxBleConnectionMock
                .setupNotification(characteristicNotifiedUUID)
                .flatMap({ Observable<byte[]> observable -> observable })
                .map { new String(it) }
                .test()

        when:
        characteristicNotificationSubject.onNext("NotificationData".getBytes())

        then:
        testSubscriber.assertValue("NotificationData")
    }

}
