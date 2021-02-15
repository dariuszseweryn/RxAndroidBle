package com.polidea.rxandroidble3.samplekotlin.example4_characteristic.advanced

import android.bluetooth.BluetoothGattCharacteristic.*
import android.os.Build
import com.polidea.rxandroidble3.RxBleDevice
import com.polidea.rxandroidble3.mockrxandroidble.RxBleClientMock
import com.polidea.rxandroidble3.samplekotlin.BuildConfig
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.subjects.PublishSubject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

private const val deviceName = "TestDevice"
private const val macAddress = "AA:BB:CC:DD:EE:FF"
private const val rssi = -42
private val serviceUUID = UUID.fromString("00001234-0000-0000-8000-000000000000")
private val characteristicUUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
private val characteristicNotifiedUUID = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
private val clientCharacteristicConfigDescriptorUuid =
    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
private val characteristicData = "Polidea".toByteArray()
private val descriptorUUID = UUID.fromString("00001337-0000-1000-8000-00805f9b34fb")
private val descriptorData = "Config".toByteArray()

@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    sdk = [Build.VERSION_CODES.LOLLIPOP]
)
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

        testObserver.assertValueSequence(
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

        testObserver.assertValueSequence(
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

        testObserver.assertValueSequence(
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

        testObserver.assertValueSequence(
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

    //region Below is copied from BaseTestConsumer from RxJava 2 — functions were removed in RxJava 3
    /**
     * Assert that the TestObserver/TestSubscriber received only items that are in the specified
     * collection as well, irrespective of the order they were received.
     *
     *
     * This helps asserting when the order of the values is not guaranteed, i.e., when merging
     * asynchronous streams.
     *
     *
     * To ensure that only the expected items have been received, no more and no less, in any order,
     * apply [.assertValueCount] with `expected.size()`.
     *
     * @param expected the collection of values expected in any order
     * @return this
     */
    private fun <T> TestObserver<T>.assertValueSet(expected: Collection<T>): TestObserver<T> {
        if (expected.isEmpty()) {
            this.assertNoValues()
            return this
        }
        for (v in this.values()) {
            if (!expected.contains(v)) {
                AssertionError("Value not in the expected collection: " + valueAndClass(v))
            }
        }
        return this
    }

    /**
     * Appends the class name to a non-null value.
     * @param o the object
     * @return the string representation
     */
    private fun valueAndClass(o: Any?): String {
        return if (o != null) {
            o.toString() + " (class: " + o.javaClass.simpleName + ")"
        } else "null"
    }
    //endregion
}
