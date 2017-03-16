package com.polidea.rxandroidble.internal.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.os.Build
import android.support.annotation.NonNull
import com.polidea.rxandroidble.*
import com.polidea.rxandroidble.exceptions.*
import com.polidea.rxandroidble.internal.operations.OperationsProviderImpl
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationReadRssi
import com.polidea.rxandroidble.internal.util.ByteAssociation
import com.polidea.rxandroidble.internal.util.CharacteristicChangedEvent
import com.polidea.rxandroidble.internal.util.MockOperationTimeoutConfiguration
import org.robolectric.annotation.Config
import org.robospock.GradleRoboSpecification
import rx.Observable
import rx.Scheduler
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import spock.lang.Unroll

import static com.polidea.rxandroidble.exceptions.BleGattOperationType.DESCRIPTOR_WRITE
import static java.util.Collections.emptyList
import static rx.Observable.from
import static rx.Observable.just

@Config(manifest = Config.NONE, constants = BuildConfig, sdk = Build.VERSION_CODES.LOLLIPOP)
class RxBleConnectionTest extends GradleRoboSpecification {
    public static final CHARACTERISTIC_UUID = UUID.fromString("f301f518-5414-471c-8a7b-2ef6d1b7373d")
    public static final CHARACTERISTIC_INSTANCE_ID = 1
    public static final OTHER_UUID = UUID.fromString("ab906173-5daa-4d6b-8604-c2be69122d57")
    public static final OTHER_INSTANCE_ID = 2
    public static final byte[] EMPTY_DATA = [] as byte[]
    public static final byte[] NOT_EMPTY_DATA = [1, 2, 3] as byte[]
    public static final byte[] OTHER_DATA = [2, 2, 3] as byte[]
    public static final int EXPECTED_RSSI_VALUE = 5
    def flatRadio = new FlatRxBleRadio()
    def gattCallback = Mock RxBleGattCallback
    def bluetoothGattMock = Mock BluetoothGatt
    def testScheduler = new TestScheduler()
    def timeoutConfig = new MockOperationTimeoutConfiguration(testScheduler)
    def operationsProviderMock = new OperationsProviderImpl(gattCallback, bluetoothGattMock, timeoutConfig, testScheduler,
            testScheduler, { new RxBleRadioOperationReadRssi(gattCallback, bluetoothGattMock, timeoutConfig) })
    def objectUnderTest = new RxBleConnectionImpl(flatRadio, gattCallback, bluetoothGattMock, operationsProviderMock,
            { new LongWriteOperationBuilderImpl(flatRadio, { 20 }, Mock(RxBleConnection)) }, testScheduler
    )
    def connectionStateChange = BehaviorSubject.create()
    def TestSubscriber testSubscriber

    def setup() {
        testSubscriber = new TestSubscriber()
        gattCallback.getOnConnectionStateChange() >> connectionStateChange
    }

    def "should emit BleGattCannotStartException if failed to start retrieving services"() {
        given:
        gattCallback.getOnServicesDiscovered() >> PublishSubject.create()
        shouldGattContainNoServices()
        shouldFailStartingDiscovery()

        when:
        objectUnderTest.discoverServices().subscribe(testSubscriber)

        then:
        testSubscriber.assertError BleGattCannotStartException
        testSubscriber.assertError { it.bleGattOperationType == BleGattOperationType.SERVICE_DISCOVERY }
    }

    @Unroll
    def "should emit BleGattCannotStartException if failed to start writing characteristic"() {
        given:
        // for third setupWriteClosure
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: OTHER_DATA)
        shouldGattContainServiceWithCharacteristic(characteristic, CHARACTERISTIC_UUID)

        gattCallback.getOnCharacteristicWrite() >> PublishSubject.create()
        shouldFailStartingCharacteristicWrite()

        when:
        setupWriteClosure.call(objectUnderTest, characteristic, OTHER_DATA).subscribe(testSubscriber)

        then:
        testSubscriber.assertError BleGattCannotStartException
        testSubscriber.assertError { it.bleGattOperationType == BleGattOperationType.CHARACTERISTIC_WRITE }

        where:
        setupWriteClosure << [
                writeCharacteristicCharacteristicDeprecatedClosure,
                writeCharacteristicCharacteristicClosure,
                writeCharacteristicUuidClosure
        ]
    }

    @Unroll
    def "should emit BleGattCannotStartException if failed to start reading characteristic"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: OTHER_DATA)
        shouldGattContainServiceWithCharacteristic(characteristic, CHARACTERISTIC_UUID)
        gattCallback.getOnCharacteristicRead() >> PublishSubject.create()
        shouldFailStartingCharacteristicRead()

        when:
        setupReadClosure.call(objectUnderTest, characteristic).subscribe(testSubscriber)

        then:
        testSubscriber.assertError BleGattCannotStartException
        testSubscriber.assertError { it.bleGattOperationType == BleGattOperationType.CHARACTERISTIC_READ }

        where:
        setupReadClosure << [
                readCharacteristicUuidClosure,
                readCharacteristicCharacteristicClosure
        ]
    }

    def "should emit BleGattCannotStartException if failed to start retrieving rssi"() {
        given:
        shouldReturnStartingStatusAndEmitRssiValueThroughCallback { false }

        when:
        objectUnderTest.readRssi().subscribe(testSubscriber)

        then:
        testSubscriber.assertError BleGattCannotStartException
        testSubscriber.assertError { it.bleGattOperationType == BleGattOperationType.READ_RSSI }
    }

    def "should return cached services during service discovery"() {
        given:
        def expectedServices = [Mock(BluetoothGattService), Mock(BluetoothGattService)]
        shouldSuccessfullyStartDiscovery()

        when:
        objectUnderTest.discoverServices().subscribe() // <-- it must be here hence mocks are not configured yet in given block.
        objectUnderTest.discoverServices().subscribe(testSubscriber)

        then:
        testSubscriber.assertServices expectedServices
        testSubscriber.assertCompleted()
        (_..1) * bluetoothGattMock.getServices() >> emptyList()
        1 * gattCallback.getOnServicesDiscovered() >> just(new RxBleDeviceServices(expectedServices))
    }

    def "should return services instantly if they were already discovered and are in BluetoothGatt cache"() {
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

    def "should try to discover services if there are no services cached within BluetoothGatt"() {
        given:
        def services = [Mock(BluetoothGattService), Mock(BluetoothGattService)]
        shouldSuccessfullyStartDiscovery()
        shouldGattContainNoServices()
        gattCallback.getOnServicesDiscovered() >> just(new RxBleDeviceServices(services))

        when:
        objectUnderTest.discoverServices().subscribe(testSubscriber)

        then:
        testSubscriber.assertServices services
        testSubscriber.assertCompleted()
        1 * bluetoothGattMock.discoverServices() >> true
    }

    def "should emit BleCharacteristicNotFoundException during read operation if no services were found"() {
        given:
        shouldGattCallbackReturnServicesOnDiscovery([])
        shouldGattContainNoServices()

        when:
        objectUnderTest.readCharacteristic(CHARACTERISTIC_UUID).subscribe(testSubscriber)

        then:
        testSubscriber.assertError BleCharacteristicNotFoundException
    }

    def "should emit BleCharacteristicNotFoundException during read operation if characteristic was not found"() {
        given:
        def service = Mock BluetoothGattService
        shouldGattCallbackReturnServicesOnDiscovery([service])
        shouldGattContainServices([service])
        service.getCharacteristic(_) >> null

        when:
        objectUnderTest.readCharacteristic(CHARACTERISTIC_UUID).subscribe(testSubscriber)

        then:
        testSubscriber.assertError BleCharacteristicNotFoundException
        testSubscriber.assertError { it.charactersisticUUID == CHARACTERISTIC_UUID }
    }

    def "should read first found characteristic with matching UUID"() {
        given:
        def service = Mock BluetoothGattService
        shouldServiceContainCharacteristic(service, CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID, NOT_EMPTY_DATA)
        shouldServiceContainCharacteristic(service, OTHER_UUID, OTHER_INSTANCE_ID, OTHER_DATA)
        shouldGattCallbackReturnServicesOnDiscovery([service])
        shouldGattContainNoServices()
        shouldGattCallbackReturnDataOnRead(
                [uuid: OTHER_UUID, value: OTHER_DATA],
                [uuid: CHARACTERISTIC_UUID, value: NOT_EMPTY_DATA])

        when:
        objectUnderTest.readCharacteristic(CHARACTERISTIC_UUID).subscribe(testSubscriber)

        then:
        testSubscriber.assertValue NOT_EMPTY_DATA
    }

    def "should emit BleCharacteristicNotFoundException if there are no services during write operation"() {
        given:
        shouldGattCallbackReturnServicesOnDiscovery([])
        shouldGattContainNoServices()

        when:
        objectUnderTest.writeCharacteristic(CHARACTERISTIC_UUID, NOT_EMPTY_DATA).subscribe(testSubscriber)

        then:
        testSubscriber.assertError BleCharacteristicNotFoundException
        testSubscriber.assertError { it.charactersisticUUID == CHARACTERISTIC_UUID }
    }

    def "should emit BleCharacteristicNotFoundException if characteristic was not found during write operation"() {
        given:
        shouldGattContainServiceWithCharacteristic(null)

        when:
        objectUnderTest.writeCharacteristic(CHARACTERISTIC_UUID, NOT_EMPTY_DATA).subscribe(testSubscriber)

        then:
        testSubscriber.assertError BleCharacteristicNotFoundException
        testSubscriber.assertError { it.charactersisticUUID == CHARACTERISTIC_UUID }
    }

    @Unroll
    def "should write characteristic and return written value"() {
        given:
        def mockedCharacteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: OTHER_DATA)
        shouldGattContainServiceWithCharacteristic(mockedCharacteristic, CHARACTERISTIC_UUID)
        def onWriteSubject = PublishSubject.create()
        gattCallback.getOnCharacteristicWrite() >> onWriteSubject

        when:
        setupWriteClosure.call(objectUnderTest, mockedCharacteristic, OTHER_DATA).subscribe(testSubscriber)

        then:
        testSubscriber.assertValue(OTHER_DATA)

        and:
        1 * bluetoothGattMock.writeCharacteristic({ it.getValue() == OTHER_DATA }) >> {
            BluetoothGattCharacteristic characteristic ->
                onWriteSubject.onNext(ByteAssociation.create(characteristic.getUuid(), characteristic.getValue()))
                true
        }

        where:
        setupWriteClosure << [
                writeCharacteristicUuidClosure,
                writeCharacteristicCharacteristicClosure
        ]
    }

    def "should emit retrieved rssi"() {
        given:
        shouldReturnStartingStatusAndEmitRssiValueThroughCallback {
            it.onNext(EXPECTED_RSSI_VALUE)
            true
        }

        when:
        objectUnderTest.readRssi().subscribe(testSubscriber)

        then:
        testSubscriber.assertValue(EXPECTED_RSSI_VALUE)
    }

    @Unroll
    def "should emit CharacteristicNotFoundException if matching characteristic wasn't found"() {
        given:
        shouldContainOneServiceWithoutCharacteristics()
        def characteristic = Mock(BluetoothGattCharacteristic)
        characteristic.getUuid() >> CHARACTERISTIC_UUID

        when:
        setupTriggerNotificationClosure.call(objectUnderTest, characteristic).flatMap({ it }).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleCharacteristicNotFoundException)
        testSubscriber.assertError { it.charactersisticUUID == CHARACTERISTIC_UUID }

        where:
        setupTriggerNotificationClosure << [
                setupNotificationUuidClosure,
                setupIndicationUuidClosure
        ]
    }

    @Unroll
    def "should emit BleCannotSetCharacteristicNotificationException if CLIENT_CONFIGURATION_DESCRIPTION wasn't found"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        characteristic.getDescriptor(_) >> null
        shouldGattContainServiceWithCharacteristic(characteristic, CHARACTERISTIC_UUID)

        when:
        setupTriggerNotificationClosure.call(objectUnderTest, characteristic).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleCannotSetCharacteristicNotificationException)

        where:
        setupTriggerNotificationClosure << [
                setupNotificationUuidClosure,
                setupIndicationUuidClosure,
                setupNotificationCharacteristicClosure,
                setupIndicationCharacteristicClosure
        ]
    }

    @Unroll
    def "should emit BleCannotSetCharacteristicNotificationException if failed to set characteristic notification"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        mockDescriptorAndAttachToCharacteristic(characteristic)
        shouldGattContainServiceWithCharacteristic(characteristic, CHARACTERISTIC_UUID)
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> false

        when:
        setupTriggerNotificationClosure.call(objectUnderTest, characteristic).flatMap({ it }).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleCannotSetCharacteristicNotificationException)

        where:
        setupTriggerNotificationClosure << [
                setupNotificationUuidClosure,
                setupIndicationUuidClosure,
                setupNotificationCharacteristicClosure,
                setupIndicationCharacteristicClosure
        ]
    }

    @Unroll
    def "should emit BleCannotSetCharacteristicNotificationException if failed to start write CLIENT_CONFIGURATION_DESCRIPTION"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        shouldGattContainServiceWithCharacteristic(characteristic, CHARACTERISTIC_UUID)
        shouldReturnStartingStatusAndEmitDescriptorWriteCallback descriptor, { false }
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true

        when:
        setupTriggerNotificationClosure.call(objectUnderTest, characteristic).flatMap({ it }).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleCannotSetCharacteristicNotificationException)

        where:
        setupTriggerNotificationClosure << [
                setupNotificationUuidClosure,
                setupIndicationUuidClosure,
                setupNotificationCharacteristicClosure,
                setupIndicationCharacteristicClosure
        ]
    }

    @Unroll
    def "should emit BleCannotSetCharacteristicNotificationException if failed to write CLIENT_CONFIGURATION_DESCRIPTION"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        shouldGattContainServiceWithCharacteristic(characteristic, CHARACTERISTIC_UUID)
        shouldReturnStartingStatusAndEmitDescriptorWriteCallback descriptor, { it.onError(new BleGattException(DESCRIPTOR_WRITE)) }
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true

        when:
        setupTriggerNotificationClosure.call(objectUnderTest, characteristic).flatMap({ it }).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleCannotSetCharacteristicNotificationException)

        where:
        setupTriggerNotificationClosure << [
                setupNotificationUuidClosure,
                setupIndicationUuidClosure,
                setupNotificationCharacteristicClosure,
                setupIndicationCharacteristicClosure
        ]
    }

    @Unroll
    def "should register notifications according to BLE standard"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        shouldGattContainServiceWithCharacteristic(characteristic, CHARACTERISTIC_UUID)
        shouldReturnStartingStatusAndEmitDescriptorWriteCallback(descriptor, { true })

        when:
        setupTriggerNotificationClosure.call(objectUnderTest, characteristic).flatMap({ it }).subscribe(testSubscriber)

        then:
        1 * bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        1 * bluetoothGattMock.writeDescriptor({ it.value == enableValue }) >> just(new byte[0])
        testSubscriber.assertNoErrors()

        where:
        setupTriggerNotificationClosure        | enableValue
        setupNotificationUuidClosure           | BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        setupIndicationUuidClosure             | BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        setupNotificationCharacteristicClosure | BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        setupIndicationCharacteristicClosure   | BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
    }

    @Unroll
    def "should register notifications according in compatibility where client descriptor is not present"() {
        given:
        gattCallback.getOnCharacteristicChanged() >> PublishSubject.create()
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        shouldGattContainServiceWithCharacteristic(characteristic, CHARACTERISTIC_UUID)

        when:
        setupTriggerNotificationClosure.call(objectUnderTest, characteristic).flatMap({ it }).subscribe(testSubscriber)

        then:
        1 * bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        0 * bluetoothGattMock.writeDescriptor() >> just(new byte[0])
        testSubscriber.assertNoErrors()

        where:
        setupTriggerNotificationClosure << [
                setupNotificationUuidCompatClosure,
                setupIndicationUuidCompatClosure,
                setupNotificationCharacteristicCompatClosure,
                setupIndicationCharacteristicCompatClosure]
    }

    @Unroll
    def "should notify about value change and stay subscribed"() {
        given:
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID)
        gattCallback.getOnCharacteristicChanged() >> from(changeNotifications.collect {
            new CharacteristicChangedEvent(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID, it)
        })

        when:
        setupTriggerNotificationClosure.call(objectUnderTest, characteristic).flatMap({ it }).subscribe(testSubscriber)

        then:
        testSubscriber.assertValues(expectedValues)
        testSubscriber.assertNotCompleted()

        where:
        // TODO
        changeNotifications          | expectedValues               | setupTriggerNotificationClosure
        [NOT_EMPTY_DATA]             | [NOT_EMPTY_DATA]             | setupNotificationUuidClosure
        [NOT_EMPTY_DATA]             | [NOT_EMPTY_DATA]             | setupIndicationUuidClosure
        [NOT_EMPTY_DATA, OTHER_DATA] | [NOT_EMPTY_DATA, OTHER_DATA] | setupNotificationUuidClosure
        [NOT_EMPTY_DATA, OTHER_DATA] | [NOT_EMPTY_DATA, OTHER_DATA] | setupIndicationUuidClosure
        [NOT_EMPTY_DATA]             | [NOT_EMPTY_DATA]             | setupNotificationCharacteristicClosure
        [NOT_EMPTY_DATA]             | [NOT_EMPTY_DATA]             | setupIndicationCharacteristicClosure
        [NOT_EMPTY_DATA, OTHER_DATA] | [NOT_EMPTY_DATA, OTHER_DATA] | setupNotificationCharacteristicClosure
        [NOT_EMPTY_DATA, OTHER_DATA] | [NOT_EMPTY_DATA, OTHER_DATA] | setupIndicationCharacteristicClosure
    }

    @Unroll
    def "should not notify about value change if UUID and / or instanceId is not matching"() {
        given:
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID)
        gattCallback.getOnCharacteristicChanged() >> just(otherCharacteristicNotificationId)

        when:
        setupTriggerNotificationClosure.call(objectUnderTest, characteristic).flatMap({ it }).subscribe(testSubscriber)

        then:
        testSubscriber.assertNoValues()
        testSubscriber.assertNotCompleted()

        where:
        [setupTriggerNotificationClosure, otherCharacteristicNotificationId] << [
                [
                        setupNotificationUuidClosure,
                        setupIndicationUuidClosure,
                        setupNotificationCharacteristicClosure,
                        setupIndicationCharacteristicClosure
                ], [
                        new CharacteristicChangedEvent(CHARACTERISTIC_UUID, OTHER_INSTANCE_ID, NOT_EMPTY_DATA),
                        new CharacteristicChangedEvent(OTHER_UUID, CHARACTERISTIC_INSTANCE_ID, NOT_EMPTY_DATA),
                        new CharacteristicChangedEvent(OTHER_UUID, OTHER_INSTANCE_ID, NOT_EMPTY_DATA)
                ]
        ].combinations()
    }

    @Unroll
    def "should reuse notification setup if UUID matches"() {
        given:
        def secondSubscriber = new TestSubscriber()
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        shouldGattContainServiceWithCharacteristic(characteristic, CHARACTERISTIC_UUID)
        gattCallback.getOnCharacteristicChanged() >> PublishSubject.create()

        def descriptorWriteSubject = shouldReturnStartingStatusAndEmitDescriptorWriteCallback(descriptor)
        bluetoothGattMock.setCharacteristicNotification(characteristic, _) >> false

        when:
        setupTriggerNotificationClosure.call(objectUnderTest, characteristic).subscribe(secondSubscriber)
        setupTriggerNotificationClosure.call(objectUnderTest, characteristic).subscribe(testSubscriber)

        then:
        1 * bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        1 * bluetoothGattMock.writeDescriptor({ it.value == enableValue }) >> {
            descriptorWriteSubject.onNext(ByteAssociation.create(descriptor, EMPTY_DATA))
            descriptorWriteSubject.onCompleted()
            true
        }

        and:
        testSubscriber.assertNoErrors()
        secondSubscriber.assertNoErrors()

        where:
        setupTriggerNotificationClosure        | enableValue
        setupNotificationUuidClosure           | BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        setupIndicationUuidClosure             | BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        setupNotificationCharacteristicClosure | BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        setupIndicationCharacteristicClosure   | BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
    }

    @Unroll
    def "should reuse notification setup if UUID matches but observable wasn't subscribed yet"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        shouldGattContainServiceWithCharacteristic(characteristic, CHARACTERISTIC_UUID)
        gattCallback.getOnCharacteristicChanged() >> PublishSubject.create()

        def descriptorWriteSubject = shouldReturnStartingStatusAndEmitDescriptorWriteCallback(descriptor)
        bluetoothGattMock.setCharacteristicNotification(characteristic, _) >> true

        def secondSubscriber = new TestSubscriber()
        def observable = setupTriggerNotificationClosure.call(objectUnderTest, characteristic)
        def secondObservable = setupTriggerNotificationClosure.call(objectUnderTest, characteristic)

        when:
        observable.subscribe(testSubscriber)
        secondObservable.subscribe(secondSubscriber)

        then:
        1 * bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        1 * bluetoothGattMock.writeDescriptor({ it.value == enableValue }) >> {
            descriptorWriteSubject.onNext(ByteAssociation.create(descriptor, EMPTY_DATA))
            descriptorWriteSubject.onCompleted()
            true
        }

        and:
        testSubscriber.assertNoErrors()
        secondSubscriber.assertNoErrors()

        where:
        setupTriggerNotificationClosure        | enableValue
        setupNotificationUuidClosure           | BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        setupIndicationUuidClosure             | BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
        setupNotificationCharacteristicClosure | BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        setupIndicationCharacteristicClosure   | BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
    }

    @Unroll
    def "should notify both subscribers about value change"() {
        given:
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID)
        def characteristicChangeSubject = PublishSubject.create()
        gattCallback.getOnCharacteristicChanged() >> characteristicChangeSubject
        def secondSubscriber = new TestSubscriber()
        setupTriggerNotificationClosure.call(objectUnderTest, characteristic).flatMap({ it }).subscribe(testSubscriber)
        setupTriggerNotificationClosure.call(objectUnderTest, characteristic).flatMap({ it }).subscribe(secondSubscriber)

        when:
        characteristicChangeSubject.onNext(new CharacteristicChangedEvent(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID, NOT_EMPTY_DATA))

        then:
        testSubscriber.assertValue(NOT_EMPTY_DATA)
        secondSubscriber.assertValue(NOT_EMPTY_DATA)

        where:
        setupTriggerNotificationClosure << [
                setupNotificationUuidClosure,
                setupIndicationUuidClosure,
                setupNotificationCharacteristicClosure,
                setupIndicationCharacteristicClosure
        ]
    }

    @Unroll
    def "should unregister notifications after all observers are unsubscribed"() {
        given:
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID)
        gattCallback.getOnCharacteristicChanged() >> PublishSubject.create()
        def secondSubscriber = new TestSubscriber()
        def firstSubscription = setupTriggerNotificationClosure.call(objectUnderTest, characteristic).flatMap({
            it
        }).subscribe(testSubscriber)
        def secondSubscription = setupTriggerNotificationClosure.call(objectUnderTest, characteristic).flatMap({
            it
        }).subscribe(secondSubscriber)

        when:
        firstSubscription.unsubscribe()

        then:
        0 * bluetoothGattMock.setCharacteristicNotification(characteristic, false) >> true
        0 * bluetoothGattMock.writeDescriptor({ it.value == BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE })

        when:
        secondSubscription.unsubscribe()

        then:
        1 * bluetoothGattMock.setCharacteristicNotification(characteristic, false) >> true
        1 * bluetoothGattMock.writeDescriptor({ it.value == BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE })

        where:
        setupTriggerNotificationClosure << [
                setupNotificationUuidClosure,
                setupIndicationUuidClosure,
                setupNotificationCharacteristicClosure,
                setupIndicationCharacteristicClosure
        ]
    }

    @Unroll
    def "should emit BleCharacteristicNotificationOfOtherTypeAlreadySetException if notification is set up after indication on the same characteristic"() {
        given:
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID)
        gattCallback.getOnCharacteristicChanged() >> PublishSubject.create()
        def secondSubscriber = new TestSubscriber()

        when:
        setupTriggerFirstNotificationClosure.call(objectUnderTest, characteristic).flatMap({ it }).subscribe(testSubscriber)
        setupTriggerSecondNotificationClosure.call(objectUnderTest, characteristic).flatMap({ it }).subscribe(secondSubscriber)

        then:
        testSubscriber.assertNoErrors()
        secondSubscriber.assertError(BleConflictingNotificationAlreadySetException)

        where:
        setupTriggerFirstNotificationClosure   | setupTriggerSecondNotificationClosure
        setupNotificationUuidClosure           | setupIndicationUuidClosure
        setupIndicationUuidClosure             | setupNotificationUuidClosure
        setupNotificationCharacteristicClosure | setupIndicationCharacteristicClosure
        setupIndicationCharacteristicClosure   | setupNotificationCharacteristicClosure
        setupNotificationUuidClosure           | setupIndicationCharacteristicClosure
        setupIndicationCharacteristicClosure   | setupNotificationUuidClosure
        setupNotificationCharacteristicClosure | setupIndicationUuidClosure
        setupIndicationUuidClosure             | setupNotificationCharacteristicClosure
    }

    def "should pass items emitted by observable returned from RxBleRadioOperationCustom.asObservable()"() {
        given:
        def radioOperationCustom = customRadioOperationWithOutcome {
            Observable.just(true, false, true)
        }

        when:
        objectUnderTest.queue(radioOperationCustom).subscribe(testSubscriber)

        then:
        testSubscriber.assertCompleted()
        testSubscriber.assertValues(true, false, true)
    }

    def "should pass error and release the radio if custom operation will throw out of RxBleRadioOperationCustom.asObservable()"() {
        given:
        def radioOperationCustom = customRadioOperationWithOutcome { throw new RuntimeException() }

        when:
        objectUnderTest.queue(radioOperationCustom).subscribe(testSubscriber)

        then:
        flatRadio.semaphore.isReleased()
        testSubscriber.assertError(RuntimeException.class)
    }

    def "should pass error and release the radio if observable returned from RxBleRadioOperationCustom.asObservable() will emit error"() {
        given:
        def radioOperationCustom = customRadioOperationWithOutcome { Observable.error(new RuntimeException()) }

        when:
        objectUnderTest.queue(radioOperationCustom).subscribe(testSubscriber)

        then:
        flatRadio.semaphore.isReleased()
        testSubscriber.assertError(RuntimeException.class)
    }

    def "should release the radio when observable returned from RxBleRadioOperationCustom.asObservable() will complete"() {
        given:
        def radioOperationCustom = customRadioOperationWithOutcome { Observable.empty() }

        when:
        objectUnderTest.queue(radioOperationCustom).subscribe(testSubscriber)

        then:
        flatRadio.semaphore.isReleased()
        testSubscriber.assertCompleted()
    }

    def "should throw illegal argument exception if RxBleRadioOperationCustom.asObservable() return null"() {
        given:
        def radioOperationCustom = customRadioOperationWithOutcome { null }

        when:
        objectUnderTest.queue(radioOperationCustom).subscribe(testSubscriber)

        then:
        flatRadio.semaphore.isReleased()
        testSubscriber.assertError(IllegalArgumentException.class)
    }

    public customRadioOperationWithOutcome(Closure<Observable<Boolean>> outcomeSupplier) {
        new RxBleRadioOperationCustom<Boolean>() {
            @NonNull
            @Override
            Observable<Boolean> asObservable(BluetoothGatt bluetoothGatt,
                                             RxBleGattCallback rxBleGattCallback,
                                             Scheduler scheduler) throws Throwable {
                outcomeSupplier()
            }
        }
    }

    public shouldSetupCharacteristicNotificationCorrectly(UUID characteristicUUID, int instanceId) {
        def characteristic = mockCharacteristicWithValue(uuid: characteristicUUID, instanceId: instanceId, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        shouldGattContainServiceWithCharacteristic(characteristic, characteristicUUID)
        shouldReturnStartingStatusAndEmitDescriptorWriteCallback(descriptor, {
            it.onNext(ByteAssociation.create(descriptor, EMPTY_DATA))
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

    public shouldGattContainServiceWithCharacteristic(BluetoothGattCharacteristic characteristic, UUID characteristicUUID = CHARACTERISTIC_UUID) {
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
        gattCallback.getOnRssiRead() >> rssiSubject
        bluetoothGattMock.readRemoteRssi() >> { closure?.call(rssiSubject) }
    }

    public PublishSubject shouldReturnStartingStatusAndEmitDescriptorWriteCallback(BluetoothGattDescriptor descriptor, Closure<Boolean> closure = null) {
        def descriptorSubject = PublishSubject.create()
        gattCallback.getOnDescriptorWrite() >> descriptorSubject
        bluetoothGattMock.writeDescriptor(descriptor) >> { closure?.call(descriptorSubject) }
        descriptorSubject
    }

    public shouldServiceContainCharacteristic(BluetoothGattService service, UUID uuid, int instanceId, byte[] characteristicValue) {
        service.getCharacteristic(uuid) >> mockCharacteristicWithValue(uuid: uuid, instanceId: instanceId, value: characteristicValue)
    }

    public shouldGattCallbackReturnDataOnRead(Map... parameters) {
        gattCallback.getOnCharacteristicRead() >> { from(parameters.collect { ByteAssociation.create it['uuid'], it['value'] }) }
    }

    public mockCharacteristicWithValue(Map characteristicData) {
        def characteristic = Mock BluetoothGattCharacteristic
        characteristic.getValue() >> characteristicData['value']
        characteristic.getUuid() >> characteristicData['uuid']
        characteristic.getInstanceId() >> characteristicData['instanceId']
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
        gattCallback.getOnServicesDiscovered() >> just(new RxBleDeviceServices(services))
    }

    public shouldFailStartingCharacteristicWrite() {
        bluetoothGattMock.writeCharacteristic(_) >> false
    }

    public shouldFailStartingCharacteristicRead() {
        bluetoothGattMock.readCharacteristic(_) >> false
    }

    private static Closure<Observable<byte[]>> readCharacteristicUuidClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic -> return connection.readCharacteristic(characteristic.getUuid()) }

    private static Closure<Observable<byte[]>> readCharacteristicCharacteristicClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic -> return connection.readCharacteristic(characteristic) }

    private static Closure<Observable<byte[]>> writeCharacteristicUuidClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic, byte[] data -> return connection.writeCharacteristic(characteristic.getUuid(), data) }

    private static Closure<Observable<byte[]>> writeCharacteristicCharacteristicClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic, byte[] data -> return connection.writeCharacteristic(characteristic, data) }

    private static Closure<Observable<byte[]>> writeCharacteristicCharacteristicDeprecatedClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic, byte[] data ->
        characteristic.setValue(data)
        return connection.writeCharacteristic(characteristic)
    }

    private static Closure<Observable<Observable<byte[]>>> setupNotificationUuidClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic -> return connection.setupNotification(characteristic.getUuid()) }

    private static Closure<Observable<Observable<byte[]>>> setupNotificationCharacteristicClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic -> return connection.setupNotification(characteristic) }

    private static Closure<Observable<Observable<byte[]>>> setupIndicationUuidClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic -> return connection.setupIndication(characteristic.getUuid()) }

    private static Closure<Observable<Observable<byte[]>>> setupIndicationCharacteristicClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic -> return connection.setupIndication(characteristic) }

    private static Closure<Observable<Observable<byte[]>>> setupNotificationUuidCompatClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic -> return connection.setupNotification(characteristic.getUuid(), NotificationSetupMode.COMPAT) }

    private static Closure<Observable<Observable<byte[]>>> setupNotificationCharacteristicCompatClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic -> return connection.setupNotification(characteristic, NotificationSetupMode.COMPAT) }

    private static Closure<Observable<Observable<byte[]>>> setupIndicationUuidCompatClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic -> return connection.setupIndication(characteristic.getUuid(), NotificationSetupMode.COMPAT) }

    private static Closure<Observable<Observable<byte[]>>> setupIndicationCharacteristicCompatClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic -> return connection.setupIndication(characteristic, NotificationSetupMode.COMPAT) }

}
