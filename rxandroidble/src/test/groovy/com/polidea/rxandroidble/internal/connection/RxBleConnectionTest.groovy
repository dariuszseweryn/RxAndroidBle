package com.polidea.rxandroidble.internal.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.os.Build
import android.support.v4.util.Pair
import com.polidea.rxandroidble.BuildConfig
import com.polidea.rxandroidble.FlatRxBleRadio
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.RxBleDeviceServices
import com.polidea.rxandroidble.exceptions.*
import org.robolectric.annotation.Config
import org.robospock.GradleRoboSpecification
import rx.observers.TestSubscriber
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import spock.lang.Unroll

import static com.polidea.rxandroidble.exceptions.BleGattOperationType.DESCRIPTOR_WRITE
import static java.util.Collections.emptyList
import static rx.Observable.from
import static rx.Observable.just

@Config(manifest = Config.NONE, constants = BuildConfig, sdk = Build.VERSION_CODES.LOLLIPOP)
class RxBleConnectionTest extends GradleRoboSpecification {
    public static final CHARACTERISTIC_UUID = UUID.randomUUID()
    public static final OTHER_UUID = UUID.randomUUID()
    public static final byte[] EMPTY_DATA = [] as byte[]
    public static final byte[] NOT_EMPTY_DATA = [1, 2, 3] as byte[]
    public static final byte[] OTHER_DATA = [2, 2, 3] as byte[]
    def flatRadio = new FlatRxBleRadio()
    def callback = Mock RxBleGattCallback
    def bluetoothGattMock = Mock BluetoothGatt
    def objectUnderTest = new RxBleConnectionImpl(flatRadio, callback, bluetoothGattMock)
    def connectionStateChange = BehaviorSubject.create()
    def TestSubscriber testSubscriber

    def setup() {
        testSubscriber = new TestSubscriber()
        callback.getOnConnectionStateChange() >> connectionStateChange
    }

    def "should proxy connection state"() {
        given:
        connectionStateChange.onNext(pushedStatus)

        when:
        objectUnderTest.getConnectionState().subscribe(testSubscriber)

        then:
        testSubscriber.assertValue(expectedState)

        where:
        pushedStatus                                       | expectedState
        RxBleConnection.RxBleConnectionState.CONNECTED     | RxBleConnection.RxBleConnectionState.CONNECTED
        RxBleConnection.RxBleConnectionState.DISCONNECTED  | RxBleConnection.RxBleConnectionState.DISCONNECTED
        RxBleConnection.RxBleConnectionState.CONNECTING    | RxBleConnection.RxBleConnectionState.CONNECTING
        RxBleConnection.RxBleConnectionState.DISCONNECTING | RxBleConnection.RxBleConnectionState.DISCONNECTING
    }

    def "should emit error if failed to start retrieving services"() {
        given:
        callback.getOnServicesDiscovered() >> PublishSubject.create()
        shouldGattContainNoServices()
        shouldFailStartingDiscovery()

        when:
        objectUnderTest.discoverServices().subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleGattCannotStartException)
        testSubscriber.assertError {
            it.bleGattOperationType == BleGattOperationType.SERVICE_DISCOVERY
        }
    }

    def "should return cached services"() {
        given:
        def services = [Mock(BluetoothGattService), Mock(BluetoothGattService)]
        shouldSuccessfullyStartDiscovery()

        when:
        objectUnderTest.discoverServices().subscribe() // <-- it must be here hence mocks are not configured yet in given block.
        objectUnderTest.discoverServices().subscribe(testSubscriber)

        then:
        testSubscriber.assertServices services
        testSubscriber.assertCompleted()
        (_..1) * bluetoothGattMock.getServices() >> emptyList()
        1 * callback.getOnServicesDiscovered() >> just(new RxBleDeviceServices(services))
    }

    def "should return services instantly if they were already discovered"() {
        given:
        def services = [Mock(BluetoothGattService), Mock(BluetoothGattService)]
        bluetoothGattMock.getServices() >> services

        when:
        objectUnderTest.discoverServices().subscribe(testSubscriber)

        then:
        testSubscriber.assertServices services
        testSubscriber.assertCompleted()
        0 * bluetoothGattMock.discoverServices()
    }

    def "should try to discover services if cached services are empty"() {
        given:
        def services = [Mock(BluetoothGattService), Mock(BluetoothGattService)]
        shouldSuccessfullyStartDiscovery()
        shouldGattContainNoServices()
        callback.getOnServicesDiscovered() >> just(new RxBleDeviceServices(services))

        when:
        objectUnderTest.discoverServices().subscribe(testSubscriber)

        then:
        testSubscriber.assertServices services
        testSubscriber.assertCompleted()
        1 * bluetoothGattMock.discoverServices() >> true
    }

    def "should emit BleCharacteristicNotFoundException if there are no services during read operation"() {
        given:
        shouldGattCallbackReturnServicesOnDiscovery([])
        shouldGattContainNoServices()

        when:
        objectUnderTest.readCharacteristic(CHARACTERISTIC_UUID).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleCharacteristicNotFoundException)
    }

    def "should emit BleCharacteristicNotFoundException if characteristic was not found during read operation"() {
        given:
        def service = Mock BluetoothGattService
        shouldGattCallbackReturnServicesOnDiscovery([service])
        shouldGattContainServices([service])
        service.getCharacteristic(_) >> null

        when:
        objectUnderTest.readCharacteristic(CHARACTERISTIC_UUID).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleCharacteristicNotFoundException)
        testSubscriber.assertError { it.charactersisticUUID == CHARACTERISTIC_UUID }
    }

    def "should read first found characteristic with matching UUID"() {
        given:
        def firstCharacteristicUUID = UUID.randomUUID()
        def secondCharacteristicUUID = UUID.randomUUID()
        def firstCharacteristicValue = [1, 2, 3] as byte[]
        def secondCharacteristicValue = [3, 4, 5] as byte[]
        def service = Mock BluetoothGattService
        shouldServiceContainCharacteristic(service, firstCharacteristicUUID, firstCharacteristicValue)
        shouldServiceContainCharacteristic(service, secondCharacteristicUUID, secondCharacteristicValue)
        shouldGattCallbackReturnServicesOnDiscovery([service])
        shouldGattContainServices([service])
        shouldGattCallbackReturnDataOnRead(
                [uuid: secondCharacteristicUUID, value: secondCharacteristicValue],
                [uuid: firstCharacteristicUUID, value: firstCharacteristicValue])

        when:
        objectUnderTest.readCharacteristic(firstCharacteristicUUID).subscribe(testSubscriber)

        then:
        testSubscriber.assertValue(firstCharacteristicValue)
    }

    def "should emit BleCharacteristicNotFoundException if there are no services during write operation"() {
        given:
        def dataToWrite = [1, 2, 3] as byte[]
        shouldGattCallbackReturnServicesOnDiscovery([])
        shouldGattContainNoServices()

        when:
        objectUnderTest.writeCharacteristic(CHARACTERISTIC_UUID, dataToWrite).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleCharacteristicNotFoundException)
        testSubscriber.assertError { it.charactersisticUUID == CHARACTERISTIC_UUID }
    }

    def "should emit BleCharacteristicNotFoundException if observable if characteristic was not found during write operation"() {
        given:
        def dataToWrite = [1, 2, 3] as byte[]
        shouldGattContainServiceWithCharacteristic(null, CHARACTERISTIC_UUID)

        when:
        objectUnderTest.writeCharacteristic(CHARACTERISTIC_UUID, dataToWrite).subscribe(testSubscriber)

        then:
        testSubscriber.assertError BleCharacteristicNotFoundException
        testSubscriber.assertError { it.charactersisticUUID == CHARACTERISTIC_UUID }
    }

    def "should emit error if failed to start retrieving rssi"() {
        given:
        shouldReturnStartingStatusAndEmitRssiValueThroughCallback { false }

        when:
        objectUnderTest.readRssi().subscribe(testSubscriber)

        then:
        testSubscriber.assertError BleGattCannotStartException
        testSubscriber.assertError { it.bleGattOperationType == BleGattOperationType.READ_RSSI }
    }

    def "should emit retrieved rssi"() {
        given:
        def expectedRssiValue = 5
        shouldReturnStartingStatusAndEmitRssiValueThroughCallback {
            it.onNext(expectedRssiValue)
            true
        }

        when:
        objectUnderTest.readRssi().subscribe(testSubscriber)

        then:
        testSubscriber.assertValue(expectedRssiValue)
    }

    def "should emit CharacteristicNotFoundException if matching characteristic wasn't found"() {
        given:
        shouldContainOneServiceWithoutCharacteristics()

        when:
        objectUnderTest.getNotification(CHARACTERISTIC_UUID).flatMap({ it }).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleCharacteristicNotFoundException)
        testSubscriber.assertError { it.charactersisticUUID == CHARACTERISTIC_UUID }
    }

    def "should emit BleCannotSetCharacteristicNotificationException if CLIENT_CONFIGURATION_DESCRIPTION wasn't found"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, value: EMPTY_DATA)
        characteristic.getDescriptor(_) >> null
        shouldGattContainServiceWithCharacteristic(characteristic, CHARACTERISTIC_UUID)

        when:
        objectUnderTest.getNotification(CHARACTERISTIC_UUID).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleCannotSetCharacteristicNotificationException)
    }

    def "should emit BleCannotSetCharacteristicNotificationException if failed to set characteristic notification"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, value: EMPTY_DATA)
        mockDescriptorAndAttachToCharacteristic(characteristic)
        shouldGattContainServiceWithCharacteristic(characteristic, CHARACTERISTIC_UUID)
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> false

        when:
        objectUnderTest.getNotification(CHARACTERISTIC_UUID).flatMap({ it }).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleCannotSetCharacteristicNotificationException)
    }

    def "should emit BleCannotSetCharacteristicNotificationException if failed to start write CLIENT_CONFIGURATION_DESCRIPTION"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        shouldGattContainServiceWithCharacteristic(characteristic, CHARACTERISTIC_UUID)
        shouldReturnStartingStatusAndEmitDescriptorWriteCallback descriptor, { false }
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true

        when:
        objectUnderTest.getNotification(CHARACTERISTIC_UUID).flatMap({ it }).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleCannotSetCharacteristicNotificationException)
    }

    def "should emit BleCannotSetCharacteristicNotificationException if failed to write CLIENT_CONFIGURATION_DESCRIPTION"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        shouldGattContainServiceWithCharacteristic(characteristic, CHARACTERISTIC_UUID)
        shouldReturnStartingStatusAndEmitDescriptorWriteCallback descriptor, { it.onError(new BleGattException(DESCRIPTOR_WRITE)) }
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true

        when:
        objectUnderTest.getNotification(CHARACTERISTIC_UUID).flatMap({ it }).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleCannotSetCharacteristicNotificationException)
    }

    def "should register notifications correctly"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        shouldGattContainServiceWithCharacteristic(characteristic, CHARACTERISTIC_UUID)
        shouldReturnStartingStatusAndEmitDescriptorWriteCallback(descriptor, { true })

        when:
        objectUnderTest.getNotification(CHARACTERISTIC_UUID).flatMap({ it }).subscribe(testSubscriber)

        then:
        1 * bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        1 * bluetoothGattMock.writeDescriptor({ it.value == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE })
    }

    @Unroll
    def "should notify about value change and stay subscribed"() {
        given:
        shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID)
        callback.getOnCharacteristicChanged() >> from(changeNotifications.collect { Pair.create(CHARACTERISTIC_UUID, it) })

        when:
        objectUnderTest.getNotification(CHARACTERISTIC_UUID).flatMap({ it }).subscribe(testSubscriber)

        then:
        testSubscriber.assertValues(expectedValues)
        testSubscriber.assertNotCompleted()

        where:
        changeNotifications          | expectedValues
        [NOT_EMPTY_DATA]             | [NOT_EMPTY_DATA]
        [NOT_EMPTY_DATA, OTHER_DATA] | [NOT_EMPTY_DATA, OTHER_DATA]
    }

    def "should not notify about value change if UUID is not matching"() {
        given:
        shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID)
        callback.getOnCharacteristicChanged() >> just(Pair.create(OTHER_UUID, NOT_EMPTY_DATA))

        when:
        objectUnderTest.getNotification(CHARACTERISTIC_UUID).flatMap({ it }).subscribe(testSubscriber)

        then:
        testSubscriber.assertNoValues()
        testSubscriber.assertNotCompleted()
    }

    def "should reuse notification setup if UUID matches"() {
        given:
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID)
        def secondSubscriber = new TestSubscriber()
        objectUnderTest.getNotification(CHARACTERISTIC_UUID).subscribe(secondSubscriber)

        when:
        objectUnderTest.getNotification(CHARACTERISTIC_UUID).subscribe(testSubscriber)

        then:
        1 * bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        1 * bluetoothGattMock.writeDescriptor({ it.value == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE })
    }

    def "should notify both subscribers about value change"() {
        given:
        shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID)
        def characteristicChangeSubject = PublishSubject.create()
        callback.getOnCharacteristicChanged() >> characteristicChangeSubject
        def secondSubscriber = new TestSubscriber()
        objectUnderTest.getNotification(CHARACTERISTIC_UUID).flatMap({ it }).subscribe(testSubscriber)
        objectUnderTest.getNotification(CHARACTERISTIC_UUID).flatMap({ it }).subscribe(secondSubscriber)

        when:
        characteristicChangeSubject.onNext(Pair.create(CHARACTERISTIC_UUID, NOT_EMPTY_DATA))

        then:
        testSubscriber.assertValue(NOT_EMPTY_DATA)
        secondSubscriber.assertValue(NOT_EMPTY_DATA)
    }

    def "should unregister notifications after all observers are unsubscribed"() {
        given:
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID)
        callback.getOnCharacteristicChanged() >> PublishSubject.create()
        def secondSubscriber = new TestSubscriber()
        def firstSubscription = objectUnderTest.getNotification(CHARACTERISTIC_UUID).flatMap({ it }).subscribe(testSubscriber)
        def secondSubscription = objectUnderTest.getNotification(CHARACTERISTIC_UUID).flatMap({ it }).subscribe(secondSubscriber)

        when:
        firstSubscription.unsubscribe()

        then:
        then:
        0 * bluetoothGattMock.setCharacteristicNotification(characteristic, false) >> true
        0 * bluetoothGattMock.writeDescriptor({ it.value == BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE })

        when:
        secondSubscription.unsubscribe()

        then:
        1 * bluetoothGattMock.setCharacteristicNotification(characteristic, false) >> true
        1 * bluetoothGattMock.writeDescriptor({ it.value == BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE })
    }

    public shouldSetupCharacteristicNotificationCorrectly(UUID characteristicUUID) {
        def characteristic = mockCharacteristicWithValue(uuid: characteristicUUID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        shouldGattContainServiceWithCharacteristic(characteristic, characteristicUUID)
        shouldReturnStartingStatusAndEmitDescriptorWriteCallback(descriptor, {
            it.onNext(Pair.create(descriptor, EMPTY_DATA))
            it.onCompleted()
            true
        })
        bluetoothGattMock.setCharacteristicNotification(characteristic, _) >> true
        characteristic
    }

    public mockDescriptorAndAttachToCharacteristic(BluetoothGattCharacteristic characteristic) {
        def descriptor = Spy(BluetoothGattDescriptor, constructorArgs: [RxBleConnectionImpl.CLIENT_CHARACTERISTIC_CONFIG_UUID, 0])
        descriptor.getCharacteristic() >> characteristic
        characteristic.getDescriptor(RxBleConnectionImpl.CLIENT_CHARACTERISTIC_CONFIG_UUID) >> descriptor
        descriptor
    }

    public shouldGattContainServiceWithCharacteristic(BluetoothGattCharacteristic characteristic, UUID characteristicUUID) {
        shouldContainOneServiceWithoutCharacteristics().getCharacteristic(characteristicUUID) >> characteristic
    }

    public shouldContainOneServiceWithoutCharacteristics() {
        def service = Mock BluetoothGattService
        shouldGattCallbackReturnServicesOnDiscovery([service])
        shouldGattContainServices([service])
        service
    }

    public shouldReturnStartingStatusAndEmitRssiValueThroughCallback(Closure<Boolean> closure) {
        def rssiSubject = PublishSubject.create()
        callback.getOnRssiRead() >> rssiSubject
        bluetoothGattMock.readRemoteRssi() >> { closure?.call(rssiSubject) }
    }

    public shouldReturnStartingStatusAndEmitDescriptorWriteCallback(BluetoothGattDescriptor descriptor, Closure<Boolean> closure) {
        def descriptorSubject = PublishSubject.create()
        callback.getOnDescriptorWrite() >> descriptorSubject
        bluetoothGattMock.writeDescriptor(descriptor) >> { closure?.call(descriptorSubject) }
    }

    public shouldServiceContainCharacteristic(BluetoothGattService service, UUID uuid, byte[] characteristicValue) {
        service.getCharacteristic(uuid) >> mockCharacteristicWithValue(uuid: uuid, value: characteristicValue)
    }

    public shouldGattCallbackReturnDataOnRead(Map... parameters) {
        callback.getOnCharacteristicRead() >> { from(parameters.collect { Pair.create it['uuid'], it['value'] }) }
    }

    public mockCharacteristicWithValue(Map characteristicData) {
        def characteristic = Mock BluetoothGattCharacteristic
        characteristic.getValue() >> characteristicData['value']
        characteristic.getUuid() >> characteristicData['uuid']
        characteristic
    }

    public shouldGattContainServices(List list) {
        bluetoothGattMock.getServices() >> list
    }

    public shouldGattContainNoServices() {
        shouldGattContainServices(emptyList())
    }

    public shouldFailStartingDiscovery() {
        bluetoothGattMock.discoverServices() >> false
    }

    public shouldSuccessfullyStartDiscovery() {
        bluetoothGattMock.discoverServices() >> true
    }

    public shouldGattCallbackReturnServicesOnDiscovery(ArrayList<BluetoothGattService> services) {
        bluetoothGattMock.discoverServices() >> true
        callback.getOnServicesDiscovered() >> just(new RxBleDeviceServices(services))
    }
}
