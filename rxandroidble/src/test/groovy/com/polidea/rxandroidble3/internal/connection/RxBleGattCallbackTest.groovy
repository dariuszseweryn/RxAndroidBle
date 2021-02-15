package com.polidea.rxandroidble2.internal.connection

import static com.polidea.rxandroidble2.RxBleConnection.RxBleConnectionState.CONNECTED

import android.bluetooth.*
import com.polidea.rxandroidble2.ConnectionParameters
import com.polidea.rxandroidble2.HiddenBluetoothGattCallback
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDeviceServices
import com.polidea.rxandroidble2.exceptions.*
import com.polidea.rxandroidble2.internal.util.ByteAssociation
import com.polidea.rxandroidble2.internal.util.CharacteristicChangedEvent
import hkhc.electricspock.ElectricSpecification
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.functions.Predicate
import io.reactivex.rxjava3.functions.Function
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import java.util.function.Consumer
import org.robolectric.annotation.Config
import spock.lang.Shared
import spock.lang.Unroll

import static android.bluetooth.BluetoothGatt.GATT_FAILURE
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS
import static android.bluetooth.BluetoothProfile.*
import static com.polidea.rxandroidble2.RxBleConnection.RxBleConnectionState.DISCONNECTED

@Config(manifest = Config.NONE)
class RxBleGattCallbackTest extends ElectricSpecification {

    DisconnectionRouter mockDisconnectionRouter

    PublishSubject mockDisconnectionSubject

    RxBleGattCallback objectUnderTest

    @Shared
    def mockBluetoothGatt = Mock BluetoothGatt

    @Shared
    def mockBluetoothGattCharacteristic = Mock BluetoothGattCharacteristic

    @Shared
    def mockBluetoothGattDescriptor = Mock BluetoothGattDescriptor

    @Shared
    def mockBluetoothDevice = Mock BluetoothDevice

    @Shared
    def mockBluetoothDeviceMacAddress = "MacAddress"

    @Shared
    def mockUuid0 = UUID.fromString("00000000-0000-0000-0000-000000000000")

    @Shared
    def mockUuid1 = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Shared
    def mockUuid2 = UUID.fromString("00000000-0000-0000-0000-000000000002")

    @Shared
    def mockUuid3 = UUID.fromString("00000000-0000-0000-0000-000000000003")

    def setupSpec() {
        mockBluetoothGatt.getDevice() >> mockBluetoothDevice
        mockBluetoothDevice.getAddress() >> mockBluetoothDeviceMacAddress
    }

    def setup() {
        mockDisconnectionRouter = Mock DisconnectionRouter
        mockDisconnectionSubject = PublishSubject.create()
        mockDisconnectionRouter.asErrorOnlyObservable() >> mockDisconnectionSubject
        objectUnderTest = new RxBleGattCallback(Schedulers.trampoline(), Mock(BluetoothGattProvider), mockDisconnectionRouter,
                new NativeCallbackDispatcher())
    }

    def "sanity check"() {

        expect:
        GATT_SUCCESS != GATT_FAILURE
    }

    @Unroll
    def "should relay BluetoothGattCallback callbacks to appropriate Observables"() {

        given:
        def testSubscriber = observableGetter.call(objectUnderTest).test()

        when:
        callbackCaller.call(objectUnderTest.getBluetoothGattCallback())

        then:
        testSubscriber.assertValueCount(1)

        where:
        observableGetter << [
                { return (it as RxBleGattCallback).getOnConnectionStateChange() },
                { return (it as RxBleGattCallback).getOnServicesDiscovered() },
                { return (it as RxBleGattCallback).getOnCharacteristicRead() },
                { return (it as RxBleGattCallback).getOnCharacteristicWrite() },
                { return (it as RxBleGattCallback).getOnCharacteristicChanged() },
                { return (it as RxBleGattCallback).getOnDescriptorRead() },
                { return (it as RxBleGattCallback).getOnDescriptorWrite() },
                { return (it as RxBleGattCallback).getOnRssiRead() },
                { return (it as RxBleGattCallback).getConnectionParametersUpdates() }
        ]
        callbackCaller << [
                { (it as BluetoothGattCallback).onConnectionStateChange(mockBluetoothGatt, GATT_SUCCESS, STATE_CONNECTED) },
                { (it as BluetoothGattCallback).onServicesDiscovered(mockBluetoothGatt, GATT_SUCCESS) },
                { (it as BluetoothGattCallback).onCharacteristicRead(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_SUCCESS) },
                { (it as BluetoothGattCallback).onCharacteristicWrite(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_SUCCESS) },
                { (it as BluetoothGattCallback).onCharacteristicChanged(mockBluetoothGatt, mockBluetoothGattCharacteristic) },
                { (it as BluetoothGattCallback).onDescriptorRead(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_SUCCESS) },
                { (it as BluetoothGattCallback).onDescriptorWrite(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_SUCCESS) },
                { (it as BluetoothGattCallback).onReadRemoteRssi(mockBluetoothGatt, 1, GATT_SUCCESS) },
                { (it as HiddenBluetoothGattCallback).onConnectionUpdated(mockBluetoothGatt, 1, 1, 1, GATT_SUCCESS) }
        ]
    }

    def "observeDisconnect() should emit error when DisconnectionRouter.asGenericObservable() emits error"() {

        given:
        def testException = new RuntimeException("test")
        def testSubscriber = objectUnderTest.observeDisconnect().test()

        when:
        mockDisconnectionSubject.onError(testException)

        then:
        testSubscriber.assertError(testException)
    }

    @Unroll
    def "should call DisconnectionRouter.onDisconnectedException() when .onConnectionStateChange() callback will receive STATE_DISCONNECTED/STATE_DISCONNECTING regardless of status"() {

        when:
        objectUnderTest.getBluetoothGattCallback().onConnectionStateChange(mockBluetoothGatt, status, state)

        then:
        1 * mockDisconnectionRouter.onDisconnectedException({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockBluetoothDeviceMacAddress })

        where:
        [state, status] << [[STATE_DISCONNECTED, STATE_DISCONNECTING], [GATT_SUCCESS, GATT_FAILURE]].combinations()
    }

    @Unroll
    def "should call DisconnectionRouter.onGattConnectionStateException() when .onConnectionStateChange() callback will receive STATE_CONNECTED/STATE_CONNECTING with status != GATT_SUCCESS "() {

        when:
        objectUnderTest.getBluetoothGattCallback().onConnectionStateChange(mockBluetoothGatt, GATT_FAILURE, state)

        then:
        1 * mockDisconnectionRouter.onGattConnectionStateException({ BleGattException e ->
            e.macAddress == mockBluetoothDeviceMacAddress &&
                    e.status == GATT_FAILURE &&
                    e.bleGattOperationType == BleGattOperationType.CONNECTION_STATE
        })

        where:
        state << [STATE_CONNECTED, STATE_CONNECTING]
    }

    @Unroll
    def "observeDisconnect() should not call DisconnectionRouter.route() if any of BluetoothGatt.on*() [other than onConnectionStateChanged()] callbacks will receive status != GATT_SUCCESS"() {

        when:
        callbackCaller.call(objectUnderTest.getBluetoothGattCallback())

        then:
        0 * mockDisconnectionRouter.onDisconnectedException(_)

        and:
        0 * mockDisconnectionRouter.onGattConnectionStateException(_)

        where:
        callbackCaller << [
                { (it as BluetoothGattCallback).onServicesDiscovered(mockBluetoothGatt, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onCharacteristicRead(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onCharacteristicWrite(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onDescriptorRead(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onDescriptorWrite(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onReadRemoteRssi(mockBluetoothGatt, 1, GATT_FAILURE) },
                { (it as HiddenBluetoothGattCallback).onConnectionUpdated(mockBluetoothGatt, 1, 1, 1, GATT_FAILURE) }
        ]
    }

    @Unroll
    def "getOnConnectionStateChange() should not throw if onConnectionStateChange() received STATE_DISCONNECTED"() {

        given:
        def testSubscriber = objectUnderTest.getOnConnectionStateChange().test()

        when:
        objectUnderTest.getBluetoothGattCallback().onConnectionStateChange(mockBluetoothGatt, status, STATE_DISCONNECTED)

        then:
        testSubscriber.assertNoErrors()
        testSubscriber.assertValue(DISCONNECTED)

        where:
        status << [
                GATT_SUCCESS,
                GATT_FAILURE
        ]
    }

    @Unroll
    def "callbacks other than getOnConnectionStateChange() should throw if DisconnectionRouter.asObservable() emits an exception"() {

        given:
        def testException = new RuntimeException("test")
        def testSubscriber = observableGetter.call(objectUnderTest).test()

        when:
        mockDisconnectionSubject.onError(testException)

        then:
        testSubscriber.assertError(testException)

        where:
        observableGetter << [
                { return (it as RxBleGattCallback).getOnServicesDiscovered() },
                { return (it as RxBleGattCallback).getOnCharacteristicRead() },
                { return (it as RxBleGattCallback).getOnCharacteristicWrite() },
                { return (it as RxBleGattCallback).getOnCharacteristicChanged() },
                { return (it as RxBleGattCallback).getOnDescriptorRead() },
                { return (it as RxBleGattCallback).getOnDescriptorWrite() },
                { return (it as RxBleGattCallback).getOnRssiRead() },
                { return (it as RxBleGattCallback).getConnectionParametersUpdates() }
        ]
    }

    @Unroll
    def "callbacks should emit error if their respective BluetoothGatt.on*() callbacks received status != GATT_SUCCESS"() {

        given:
        def testSubscriber = observablePicker.call(objectUnderTest).test()

        when:
        callbackCaller.call(objectUnderTest.getBluetoothGattCallback())

        then:
        errorAssertion.call(testSubscriber)

        where:
        observablePicker << [
                { (it as RxBleGattCallback).getOnServicesDiscovered() },
                { (it as RxBleGattCallback).getOnCharacteristicRead() },
                { (it as RxBleGattCallback).getOnCharacteristicWrite() },
                { (it as RxBleGattCallback).getOnDescriptorRead() },
                { (it as RxBleGattCallback).getOnDescriptorWrite() },
                { (it as RxBleGattCallback).getOnRssiRead() },
                { (it as RxBleGattCallback).getConnectionParametersUpdates() }
        ]
        callbackCaller << [
                { (it as BluetoothGattCallback).onServicesDiscovered(mockBluetoothGatt, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onCharacteristicRead(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onCharacteristicWrite(mockBluetoothGatt, mockBluetoothGattCharacteristic, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onDescriptorRead(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onDescriptorWrite(mockBluetoothGatt, mockBluetoothGattDescriptor, GATT_FAILURE) },
                { (it as BluetoothGattCallback).onReadRemoteRssi(mockBluetoothGatt, 1, GATT_FAILURE) },
                { (it as HiddenBluetoothGattCallback).onConnectionUpdated(mockBluetoothGatt, 1, 1, 1, GATT_FAILURE) }
        ]
        errorAssertion << [
                {
                    (it as TestObserver).assertError {
                        it instanceof BleGattException && it.getMacAddress() == mockBluetoothDeviceMacAddress
                    }
                },
                {
                    (it as TestObserver).assertError {
                        it instanceof BleGattCharacteristicException && it.characteristic == mockBluetoothGattCharacteristic && it.getMacAddress() == mockBluetoothDeviceMacAddress
                    }
                },
                {
                    (it as TestObserver).assertError {
                        it instanceof BleGattCharacteristicException && it.characteristic == mockBluetoothGattCharacteristic && it.getMacAddress() == mockBluetoothDeviceMacAddress
                    }
                },
                {
                    (it as TestObserver).assertError {
                        it instanceof BleGattDescriptorException && it.descriptor == mockBluetoothGattDescriptor && it.getMacAddress() == mockBluetoothDeviceMacAddress
                    }
                },
                {
                    (it as TestObserver).assertError {
                        it instanceof BleGattDescriptorException && it.descriptor == mockBluetoothGattDescriptor && it.getMacAddress() == mockBluetoothDeviceMacAddress
                    }
                },
                {
                    (it as TestObserver).assertError {
                        it instanceof BleGattException && it.getMacAddress() == mockBluetoothDeviceMacAddress
                    }
                },
                {
                    (it as TestObserver).assertError {
                        it instanceof BleGattException && it.getMacAddress() == mockBluetoothDeviceMacAddress
                    }
                }
        ]
    }

    @Unroll
    def "should transmit error on proper callback when status != BluetoothGatt.GATT_SUCCESS, subsequent calls to callbacks will work normally"() {

        given:
        def testSubscriber = givenSubscription.call(objectUnderTest).test()

        when:
        whenAction.call(objectUnderTest.getBluetoothGattCallback(), GATT_FAILURE)

        then:
        testSubscriber.assertError({ it instanceof BleGattException && ((BleGattException) it).status == GATT_FAILURE })

        when:
        def secondTestSubscriber = givenSubscription.call(objectUnderTest).test()
        whenAction.call(objectUnderTest.getBluetoothGattCallback(), GATT_SUCCESS)

        then:
        secondTestSubscriber.assertValueCount(1)

        where:
        givenSubscription << [
                { RxBleGattCallback objectUnderTest -> objectUnderTest.getOnCharacteristicRead() },
                { RxBleGattCallback objectUnderTest -> objectUnderTest.getOnCharacteristicWrite() },
                { RxBleGattCallback objectUnderTest -> objectUnderTest.getOnDescriptorRead() },
                { RxBleGattCallback objectUnderTest -> objectUnderTest.getOnDescriptorWrite() },
                { RxBleGattCallback objectUnderTest -> objectUnderTest.getOnRssiRead() },
                { RxBleGattCallback objectUnderTest -> objectUnderTest.getOnServicesDiscovered() },
                { RxBleGattCallback objectUnderTest -> objectUnderTest.getConnectionParametersUpdates() }
        ]
        whenAction << [
                { BluetoothGattCallback callback, int status -> callback.onCharacteristicRead(mockBluetoothGatt, Mock(BluetoothGattCharacteristic), status) },
                { BluetoothGattCallback callback, int status -> callback.onCharacteristicWrite(mockBluetoothGatt, Mock(BluetoothGattCharacteristic), status) },
                { BluetoothGattCallback callback, int status -> callback.onDescriptorRead(mockBluetoothGatt, Mock(BluetoothGattDescriptor), status) },
                { BluetoothGattCallback callback, int status -> callback.onDescriptorWrite(mockBluetoothGatt, Mock(BluetoothGattDescriptor), status) },
                { BluetoothGattCallback callback, int status -> callback.onReadRemoteRssi(mockBluetoothGatt, 0, status) },
                { BluetoothGattCallback callback, int status -> callback.onServicesDiscovered(mockBluetoothGatt, status) },
                { BluetoothGattCallback callback, int status -> (callback as HiddenBluetoothGattCallback).onConnectionUpdated(mockBluetoothGatt, 1, 1, 1, status) }
        ]
    }

    def "notifications should maintain the original order after filtering"() {
        given:
        def testScheduler = new TestScheduler()
        objectUnderTest = new RxBleGattCallback(testScheduler, Mock(BluetoothGattProvider), mockDisconnectionRouter,
                new NativeCallbackDispatcher())

        def testObserver = Observable.merge(
                objectUnderTest.getOnCharacteristicChanged().filter({ it.second == 0 }),
                objectUnderTest.getOnCharacteristicChanged().filter({ it.second == 1 })
        )
                .test()

        objectUnderTest.getBluetoothGattCallback().onCharacteristicChanged(mockBluetoothGatt, mockCharacteristicWithId(1))
        objectUnderTest.getBluetoothGattCallback().onCharacteristicChanged(mockBluetoothGatt, mockCharacteristicWithId(0))

        when:
        testScheduler.triggerActions()

        then:
        testObserver.assertValueAt(0, { it.second == 1 } as Predicate)
        testObserver.assertValueAt(1, { it.second == 0 } as Predicate)
    }

    @Shared
    def callbackTestCases = [
            new CallbackTestCase(
                    "Notification0",
                    { RxBleGattCallback out -> out.getOnCharacteristicChanged().filter({ it.second == 0 }) },
                    { BluetoothGattCallback bgc -> bgc.onCharacteristicChanged(mockBluetoothGatt, mockCharacteristicWithId(0)) },
                    { it instanceof CharacteristicChangedEvent && it.second == 0 }
            ),
            new CallbackTestCase(
                    "Notification1",
                    { RxBleGattCallback out -> out.getOnCharacteristicChanged().filter({ it.second == 1 }) },
                    { BluetoothGattCallback bgc -> bgc.onCharacteristicChanged(mockBluetoothGatt, mockCharacteristicWithId(1)) },
                    { it instanceof CharacteristicChangedEvent && it.second == 1 }
            ),
            new CallbackTestCase(
                    "CharacteristicRead",
                    { RxBleGattCallback out -> out.getOnCharacteristicRead() },
                    { BluetoothGattCallback bgc -> bgc.onCharacteristicRead(mockBluetoothGatt, mockCharacteristicWithUuid(mockUuid0), GATT_SUCCESS) },
                    { it instanceof ByteAssociation && it.first == mockUuid0 }
            ),
            new CallbackTestCase(
                    "CharacteristicWrite",
                    { RxBleGattCallback out -> out.getOnCharacteristicWrite() },
                    { BluetoothGattCallback bgc -> bgc.onCharacteristicWrite(mockBluetoothGatt, mockCharacteristicWithUuid(mockUuid1), GATT_SUCCESS) },
                    { it instanceof ByteAssociation && it.first == mockUuid1 }
            ),
            new CallbackTestCase(
                    "ConnectionState",
                    { RxBleGattCallback out -> out.getOnConnectionStateChange() },
                    { BluetoothGattCallback bgc -> bgc.onConnectionStateChange(mockBluetoothGatt, GATT_SUCCESS, STATE_CONNECTED) },
                    { it instanceof RxBleConnection.RxBleConnectionState && it == CONNECTED }
            ),
            new CallbackTestCase(
                    "DescriptorRead",
                    { RxBleGattCallback out -> out.getOnDescriptorRead() },
                    { BluetoothGattCallback bgc -> bgc.onDescriptorRead(mockBluetoothGatt, mockDescriptorWithUuid(mockUuid2), GATT_SUCCESS) },
                    { it instanceof ByteAssociation && it.first.getUuid() == mockUuid2 }
            ),
            new CallbackTestCase(
                    "DescriptorWrite",
                    { RxBleGattCallback out -> out.getOnDescriptorWrite() },
                    { BluetoothGattCallback bgc -> bgc.onDescriptorWrite(mockBluetoothGatt, mockDescriptorWithUuid(mockUuid3), GATT_SUCCESS) },
                    { it instanceof ByteAssociation && it.first.getUuid() == mockUuid3 }
            ),
            new CallbackTestCase(
                    "MTU",
                    { RxBleGattCallback out -> out.getOnMtuChanged() },
                    { BluetoothGattCallback bgc -> bgc.onMtuChanged(mockBluetoothGatt, 1337, GATT_SUCCESS) },
                    { it instanceof Integer && it == 1337 }
            ),
            new CallbackTestCase(
                    "RSSI",
                    { RxBleGattCallback out -> out.getOnRssiRead() },
                    { BluetoothGattCallback bgc -> bgc.onReadRemoteRssi(mockBluetoothGatt, 13373, GATT_SUCCESS) },
                    { it instanceof Integer && it == 13373 }
            ),
            new CallbackTestCase(
                    "ServiceDiscovery",
                    { RxBleGattCallback out -> out.getOnServicesDiscovered() },
                    { BluetoothGattCallback bgc -> bgc.onServicesDiscovered(mockBluetoothGatt, GATT_SUCCESS) },
                    { it instanceof RxBleDeviceServices }
            ),
            new CallbackTestCase(
                    "ConnectionParameters",
                    { RxBleGattCallback out -> out.getConnectionParametersUpdates() },
                    { BluetoothGattCallback bgc -> (bgc as HiddenBluetoothGattCallback).onConnectionUpdated(mockBluetoothGatt, 1, 2, 3, GATT_SUCCESS) },
                    { it instanceof ConnectionParameters }
            ),
    ]

    @Unroll
    def "callbacks should maintain the original order (First call = #ctc1, Second call = #ctc0)"() {
        given:
        def testScheduler = new TestScheduler()
        CallbackTestCase testCase0 = ctc0
        CallbackTestCase testCase1 = ctc1
        objectUnderTest = new RxBleGattCallback(testScheduler, Mock(BluetoothGattProvider), mockDisconnectionRouter,
                new NativeCallbackDispatcher())

        def testObserver = Observable.merge(
                testCase0.getSubscriber().apply(objectUnderTest),
                testCase1.getSubscriber().apply(objectUnderTest)
        ).test()

        // from now inverted order!
        testCase1.getAction().accept(objectUnderTest.getBluetoothGattCallback())
        testCase0.getAction().accept(objectUnderTest.getBluetoothGattCallback())

        when:
        testScheduler.triggerActions()

        then:
        testObserver.assertValueAt(0, (Predicate) testCase1.getPredicate())
        testObserver.assertValueAt(1, (Predicate) testCase0.getPredicate())

        where:
        [ctc0, ctc1] << [callbackTestCases, callbackTestCases].combinations()
    }

    BluetoothGattCharacteristic mockCharacteristicWithId(Integer id) {
        def characteristic = Mock(BluetoothGattCharacteristic)
        characteristic.getUuid() >> UUID.randomUUID()
        characteristic.getInstanceId() >> id
        characteristic.getValue() >> []
        return characteristic
    }

    BluetoothGattCharacteristic mockCharacteristicWithUuid(UUID uuid) {
        def characteristic = Mock(BluetoothGattCharacteristic)
        characteristic.getUuid() >> uuid
        characteristic.getInstanceId() >> 0
        characteristic.getValue() >> []
        return characteristic
    }

    BluetoothGattDescriptor mockDescriptorWithUuid(UUID uuid) {
        def descriptor = Mock(BluetoothGattDescriptor)
        descriptor.getUuid() >> uuid
        descriptor.getValue() >> []
        return descriptor
    }

    static class CallbackTestCase {

        private final String name

        private final Function<RxBleGattCallback, Observable> subscriber

        private final Consumer<BluetoothGattCallback> action

        private final Predicate predicate

        CallbackTestCase(
                String name,
                Function<RxBleGattCallback, Observable> subscriber,
                Consumer<BluetoothGattCallback> action,
                Predicate predicate
        ) {
            this.name = name
            this.subscriber = subscriber
            this.action = action
            this.predicate = predicate
        }

        Function<RxBleGattCallback, Observable> getSubscriber() {
            return subscriber
        }

        Consumer<BluetoothGattCallback> getAction() {
            return action
        }

        Predicate getPredicate() {
            return new Predicate() {

                @Override
                boolean test(@NonNull Object o) throws Exception {
                    if (!CallbackTestCase.this.@predicate.test(o)) {
                        throw new AssertionError("Unexpected emission found. (Emissions received in wrong order)")
                    }
                    return true
                }
            }
        }

        @Override
        String toString() {
            return "CallbackTestCase{" +
                    "name='" + name + '\'' +
                    '}'
        }
    }
}
