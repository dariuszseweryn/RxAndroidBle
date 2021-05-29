package com.polidea.rxandroidble2.samplekotlin.example4_characteristic.advanced

import android.bluetooth.BluetoothGattCharacteristic.*
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.mockrxandroidble.RxBleClientMock
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.BlockJUnit4ClassRunner
import java.util.*

private const val deviceName = "TestDevice"
private const val macAddress = "AA:BB:CC:DD:EE:FF"
private const val rssi = -42
private val serviceUUID = UUID.fromString("00001234-0000-0000-8000-000000000000")
private val characteristicUUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
private val characteristicNotifiedUUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
private val clientCharacteristicConfigDescriptorUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
private val characteristicData = "Polidea".toByteArray()
private val descriptorUUID = UUID.fromString("00001337-0000-1000-8000-00805f9b34fb")
private val descriptorData = "Config".toByteArray()

@RunWith(BlockJUnit4ClassRunner::class)
class PresenterTest {

    private val characteristicNotificationSubject = PublishSubject.create<ByteArray>()

    private val connectClicks = PublishSubject.create<Boolean>()
    private val connectingClicks = PublishSubject.create<Boolean>()
    private val disconnectClicks = PublishSubject.create<Boolean>()
    private val readClicks = PublishSubject.create<Boolean>()
    private val writeClicks = PublishSubject.create<ByteArray>()
    private val enableNotifyClicks = PublishSubject.create<Boolean>()
    private val enablingNotifyClicks = PublishSubject.create<Boolean>()
    private val disableNotifyClicks = PublishSubject.create<Boolean>()
    private val enableIndicateClicks = PublishSubject.create<Boolean>()
    private val enablingIndicateClicks = PublishSubject.create<Boolean>()
    private val disableIndicateClicks = PublishSubject.create<Boolean>()

    @Test
    fun `when click read then read characteristic`() {
        val device = createDevice(deviceName, macAddress, rssi, PROPERTY_READ)
        val testObserver = presenterEventObservable(device).test()

        connectClicks.onNext(true)
        readClicks.onNext(true)

        testObserver.assertValueSet(
            listOf(
                InfoEvent("Hey, connection has been established!"),
                CompatibilityModeEvent(false),
                ResultEvent(characteristicData.toList(), Type.READ)
            )
        ).assertValueCount(3)
    }

    @Test
    fun `when click write then write characteristic`() {
        val device = createDevice(deviceName, macAddress, rssi, PROPERTY_WRITE)
        val testObserver = presenterEventObservable(device).test()

        connectClicks.onNext(true)
        writeClicks.onNext("TestWrite".toByteArray())

        testObserver.assertValueSet(
            listOf(
                InfoEvent("Hey, connection has been established!"),
                CompatibilityModeEvent(false),
                ResultEvent("TestWrite".toByteArray().toList(), Type.WRITE)
            )
        ).assertValueCount(3)
    }

    @Test
    fun `when click notify then notify characteristic`() {
        val device = createDevice(deviceName, macAddress, rssi, PROPERTY_NOTIFY)
        val testObserver = presenterEventObservable(device).test()

        connectClicks.onNext(true)
        enableNotifyClicks.onNext(true)
        characteristicNotificationSubject.onNext("TestNotification".toByteArray())

        testObserver.assertValueSet(
            listOf(
                InfoEvent("Hey, connection has been established!"),
                CompatibilityModeEvent(false),
                ResultEvent("TestNotification".toByteArray().toList(), Type.NOTIFY)
            )
        ).assertValueCount(3)
    }

    @Test
    fun `when click indicate then indicate characteristic`() {
        val device = createDevice(deviceName, macAddress, rssi, PROPERTY_INDICATE)
        val testObserver = presenterEventObservable(device).test()

        connectClicks.onNext(true)
        enableIndicateClicks.onNext(true)
        characteristicNotificationSubject.onNext("TestIndication".toByteArray())

        testObserver.assertValueSet(
            listOf(
                InfoEvent("Hey, connection has been established!"),
                CompatibilityModeEvent(false),
                ResultEvent("TestIndication".toByteArray().toList(), Type.INDICATE)
            )
        ).assertValueCount(3)
    }

    /**
     * Creates and configures a mock BLE device.
     */
    private fun createDevice(
        deviceName: String,
        macAddress: String,
        rssi: Int,
        characteristicProperties: Int
    ): RxBleDevice =
        RxBleClientMock.DeviceBuilder()
            .deviceMacAddress(macAddress)
            .deviceName(deviceName)
            .scanRecord("ScanRecord".toByteArray())
            .rssi(rssi)
            .notificationSource(characteristicNotifiedUUID, characteristicNotificationSubject)
            .addService(
                serviceUUID,
                RxBleClientMock.CharacteristicsBuilder()
                    .addCharacteristic(
                        characteristicUUID,
                        characteristicData,
                        characteristicProperties,
                        RxBleClientMock.DescriptorsBuilder()
                            .addDescriptor(clientCharacteristicConfigDescriptorUuid, descriptorData)
                            .build()
                    )
                    .build()
            ).build()

    /**
     * Creates the presenter instance being tested.
     */
    private fun presenterEventObservable(device: RxBleDevice): Observable<PresenterEvent> =
        prepareActivityLogic(
            device,
            characteristicUUID,
            connectClicks,
            connectingClicks,
            disconnectClicks,
            readClicks,
            writeClicks,
            enableNotifyClicks,
            enablingNotifyClicks,
            disableNotifyClicks,
            enableIndicateClicks,
            enablingIndicateClicks,
            disableIndicateClicks
        )
}
