package com.polidea.rxandroidble2.internal.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.polidea.rxandroidble2.NotificationSetupMode
import com.polidea.rxandroidble2.exceptions.BleCannotSetCharacteristicNotificationException
import com.polidea.rxandroidble2.exceptions.BleConflictingNotificationAlreadySetException
import com.polidea.rxandroidble2.internal.util.CharacteristicChangedEvent
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.robolectric.annotation.Config
import org.robospock.RoboSpecification
import spock.lang.Unroll

import static io.reactivex.Observable.just

@Config(manifest = Config.NONE)
class NotificationAndIndicationManagerTest extends RoboSpecification {

    public static final CHARACTERISTIC_UUID = UUID.fromString("f301f518-5414-471c-8a7b-2ef6d1b7373d")
    public static final CHARACTERISTIC_INSTANCE_ID = 1
    public static final OTHER_UUID = UUID.fromString("ab906173-5daa-4d6b-8604-c2be69122d57")
    public static final OTHER_INSTANCE_ID = 2
    public static final byte[] EMPTY_DATA = [] as byte[]
    public static final byte[] NOT_EMPTY_DATA = [1, 2, 3] as byte[]
    public static final byte[] OTHER_DATA = [2, 2, 3] as byte[]
    public static final byte[] ENABLE_NOTIFICATION_VALUE = [1] as byte[]
    public static final byte[] ENABLE_INDICATION_VALUE = [2] as byte[]
    public static final byte[] DISABLE_NOTIFICATION_VALUE = [3] as byte[]
    public static final boolean[] ACK_VALUES = [true, false]
    public static final NotificationSetupMode[] MODES = [NotificationSetupMode.DEFAULT, NotificationSetupMode.COMPAT]
    def bluetoothGattMock = Mock(BluetoothGatt)
    def rxBleGattCallbackMock = Mock(RxBleGattCallback)
    def descriptorWriterMock = Mock(DescriptorWriter)
    NotificationAndIndicationManager objectUnderTest
    def disconnectedErrorBehaviourSubject = BehaviorSubject.create()

    def setup() {
        rxBleGattCallbackMock.observeDisconnect() >> disconnectedErrorBehaviourSubject
        objectUnderTest = new NotificationAndIndicationManager(
                ENABLE_NOTIFICATION_VALUE,
                ENABLE_INDICATION_VALUE,
                DISABLE_NOTIFICATION_VALUE,
                bluetoothGattMock,
                rxBleGattCallbackMock,
                descriptorWriterMock)
    }

    @Unroll
    def "should emit BleCannotSetCharacteristicNotificationException with CANNOT_FIND_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR reason if CLIENT_CONFIGURATION_DESCRIPTION wasn't found when in DEFAULT mode"() {

        given:
        descriptorWriterMock.writeDescriptor(_, _) >> Single.just(EMPTY_DATA)
        bluetoothGattMock.setCharacteristicNotification(_, _) >> true
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.empty()
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        characteristic.getDescriptor(_) >> null

        when:
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, NotificationSetupMode.DEFAULT, ack).test()

        then:
        testSubscriber.assertError {
            BleCannotSetCharacteristicNotificationException e -> e.getReason() == BleCannotSetCharacteristicNotificationException.CANNOT_FIND_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR
        }

        where:
        ack << ACK_VALUES
    }

    @Unroll
    def "should setup notification even if CLIENT_CONFIGURATION_DESCRIPTION wasn't found when in COMPAT mode"() {

        given:
        descriptorWriterMock.writeDescriptor(_, _) >> Single.just(EMPTY_DATA)
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.empty()
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        characteristic.getDescriptor(_) >> null

        when:
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, NotificationSetupMode.COMPAT, ack).test()

        then:
        1 * bluetoothGattMock.setCharacteristicNotification(_, _) >> true
        testSubscriber.assertValueCount(1)

        where:
        ack << ACK_VALUES
    }

    @Unroll
    def "should emit BleCannotSetCharacteristicNotificationException with CANNOT_SET_LOCAL_NOTIFICATION reason if failed to set characteristic notification"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        descriptorWriterMock.writeDescriptor(_, _) >> Single.just(EMPTY_DATA)
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.empty()
        mockDescriptorAndAttachToCharacteristic(characteristic)
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> false

        when:
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack).test()

        then:
        testSubscriber.assertError {
            BleCannotSetCharacteristicNotificationException e -> e.getReason() == BleCannotSetCharacteristicNotificationException.CANNOT_SET_LOCAL_NOTIFICATION
        }

        where:
        [ack, mode] << [ACK_VALUES, MODES].combinations()
    }

    @Unroll
    def "should emit BleCannotSetCharacteristicNotificationException with CANNOT_WRITE_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR reason and a cause if failed to write successfully CCC Descriptor when in DEFAULT mode"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.empty()
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        def testExceptionCause = new RuntimeException()
        descriptorWriterMock.writeDescriptor(descriptor, _) >> Single.error(testExceptionCause)

        when:
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, NotificationSetupMode.DEFAULT, ack).test()

        then:
        testSubscriber.assertError {
            BleCannotSetCharacteristicNotificationException e ->
                e.getReason() == BleCannotSetCharacteristicNotificationException.CANNOT_WRITE_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR &&
                        e.getCause() == testExceptionCause
        }

        where:
        ack << ACK_VALUES
    }

    @Unroll
    def "should proxy RxBleGattCallback.observeDisconnect() if happened before .subscribe()"() {
        given:
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID)
        def testException = new RuntimeException("test")
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.never()
        disconnectedErrorBehaviourSubject.onError(testException)

        when:
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack).test()

        then:
        testSubscriber.assertError(testException)

        where:
        [mode, ack] << [MODES, ACK_VALUES].combinations()
    }

    @Unroll
    def "should proxy RxBleGattCallback.observeDisconnect() if happened after io.reactivex.Observable<byte[]> emission"() {
        given:
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID)
        def testException = new RuntimeException("test")
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.never()
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack).test()

        when:
        disconnectedErrorBehaviourSubject.onError(testException)

        then:
        testSubscriber.assertValueCount(1)
        testSubscriber.assertError(testException)

        where:
        [mode, ack] << [MODES, ACK_VALUES].combinations()
    }

    @Unroll
    def "should write proper value to CCC Descriptor when in DEFAULT mode"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true

        when:
        objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, NotificationSetupMode.DEFAULT, ack).test()

        then:
        1 * descriptorWriterMock.writeDescriptor(descriptor, value) >> Single.just(EMPTY_DATA)

        where:
        ack << ACK_VALUES
        value << [ENABLE_INDICATION_VALUE, ENABLE_NOTIFICATION_VALUE]
    }

    @Unroll
    def "should notify about value change and stay subscribed"() {
        given:
        println("Values: " + changeNotificationsAndExpectedValues + " mode: " + mode + " ack: " + ack)
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID)
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.fromIterable(changeNotificationsAndExpectedValues.collect {
            new CharacteristicChangedEvent(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID, it)
        })

        when:
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack).flatMap({ it }).test()

        then:
        testSubscriber.assertValues(changeNotificationsAndExpectedValues)
        testSubscriber.assertNotTerminated()

        where:
        [changeNotificationsAndExpectedValues, mode, ack] << [
                [[NOT_EMPTY_DATA], [NOT_EMPTY_DATA, OTHER_DATA]],
                MODES,
                ACK_VALUES
        ].combinations()
    }

    @Unroll
    def "should not notify about value change if UUID and / or instanceId is not matching"() {
        given:
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID)
        bluetoothGattMock.getOnCharacteristicChanged() >> just(otherCharacteristicNotificationId)

        when:
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack).flatMap({ it }).test()

        then:
        testSubscriber.assertNoValues()
        testSubscriber.assertNotComplete()

        where:
        [mode, ack, otherCharacteristicNotificationId] << [
                MODES,
                ACK_VALUES,
                [
                        new CharacteristicChangedEvent(CHARACTERISTIC_UUID, OTHER_INSTANCE_ID, NOT_EMPTY_DATA),
                        new CharacteristicChangedEvent(OTHER_UUID, CHARACTERISTIC_INSTANCE_ID, NOT_EMPTY_DATA),
                        new CharacteristicChangedEvent(OTHER_UUID, OTHER_INSTANCE_ID, NOT_EMPTY_DATA)
                ]
        ].combinations()
    }

    @Unroll
    def "should not setup another notification if one was already done on the same characteristic"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> PublishSubject.create()
        descriptorWriterMock.writeDescriptor(descriptor, _) >> Single.just(EMPTY_DATA)

        when:
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack).test()
        def secondSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack).test()

        then:
        1 * bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true

        and:
        testSubscriber.assertValueCount(1)
        testSubscriber.assertNoErrors()
        secondSubscriber.assertValueCount(1)
        secondSubscriber.assertNoErrors()

        where:
        [mode, ack] << [MODES, ACK_VALUES].combinations()
    }

    @Unroll
    def "should not setup another notification if one was already done on the same characteristic even if not subscribed yet"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> PublishSubject.create()
        descriptorWriterMock.writeDescriptor(descriptor, _) >> Single.just(EMPTY_DATA)
        def firstObservable = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack)
        def secondObservable = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack)

        when:
        def testSubscriber = firstObservable.test()
        def secondSubscriber = secondObservable.test()

        then:
        1 * bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true

        and:
        testSubscriber.assertValueCount(1)
        testSubscriber.assertNoErrors()
        secondSubscriber.assertValueCount(1)
        secondSubscriber.assertNoErrors()

        where:
        [mode, ack] << [MODES, ACK_VALUES].combinations()
    }

    @Unroll
    def "should notify both subscribers about value change"() {
        given:
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID)
        def characteristicChangeSubject = PublishSubject.create()
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> characteristicChangeSubject
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack).flatMap({ it }).test()
        def secondSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack).flatMap({ it }).test()

        when:
        characteristicChangeSubject.onNext(new CharacteristicChangedEvent(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID, NOT_EMPTY_DATA))

        then:
        testSubscriber.assertValue(NOT_EMPTY_DATA)
        secondSubscriber.assertValue(NOT_EMPTY_DATA)

        where:
        [mode, ack] << [MODES, ACK_VALUES].combinations()
    }

    @Unroll
    def "should unregister notifications after all observers are unsubscribed"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        1 * bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> PublishSubject.create()

        when:
        def firstSubscription = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack).test()
        def secondSubscription = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack).test()

        then:
        writerCalls * descriptorWriterMock.writeDescriptor(descriptor, { it == enableValue }) >> Single.just(EMPTY_DATA)

        when:
        firstSubscription.dispose()

        then:
        0 * bluetoothGattMock.setCharacteristicNotification(characteristic, false) >> true
        0 * descriptorWriterMock.writeDescriptor(descriptor, _) >> Single.just(EMPTY_DATA)

        when:
        secondSubscription.dispose()

        then:
        1 * bluetoothGattMock.setCharacteristicNotification(characteristic, false) >> true
        writerCalls * descriptorWriterMock.writeDescriptor(descriptor, { it == DISABLE_NOTIFICATION_VALUE }) >> Single.just(EMPTY_DATA)

        where:
        mode                          | ack   | writerCalls | enableValue
        NotificationSetupMode.DEFAULT | true  | 1           | ENABLE_INDICATION_VALUE
        NotificationSetupMode.COMPAT  | true  | 0           | ENABLE_INDICATION_VALUE
        NotificationSetupMode.DEFAULT | false | 1           | ENABLE_NOTIFICATION_VALUE
        NotificationSetupMode.COMPAT  | false | 0           | ENABLE_NOTIFICATION_VALUE
    }

    @Unroll
    def "should emit BleCharacteristicNotificationOfOtherTypeAlreadySetException if notification is set up after indication on the same characteristic"() {
        given:
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID)
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> PublishSubject.create()
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode0, acks[0]).test()

        when:
        def secondSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode1, acks[1]).test()

        then:
        testSubscriber.assertNoErrors()
        secondSubscriber.assertError(BleConflictingNotificationAlreadySetException)

        where:
        [mode0, mode1, acks] << [
                MODES,
                MODES,
                [[true, false], [false, true]]
        ].combinations()
    }

    @Unroll
    def "should complete the emitted io.reactivex.Observable<byte> when unsubscribed"() {
        given:
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID)
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.never()
        def emittedObservableSubscriber
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack)
                .doOnNext { emittedObservableSubscriber = it.test() }
                .test()

        when:
        testSubscriber.dispose()

        then:
        emittedObservableSubscriber.assertComplete()

        where:
        [mode, ack] << [MODES, ACK_VALUES].combinations()
    }

    @Unroll
    def "should proxy the error emitted by RxBleGattCallback.getOnCharacteristicChanged() to emitted io.reactivex.Observable<byte>"() {
        given:
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID)
        def testException = new RuntimeException("test")
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.error(testException)
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack)
                .doOnNext { it.test() }
                .test()

        when:
        disconnectedErrorBehaviourSubject.onError(testException)

        then:
        testSubscriber.assertError(testException)

        where:
        [mode, ack] << [MODES, ACK_VALUES].combinations()
    }

    public mockCharacteristicWithValue(Map characteristicData) {
        def characteristic = Mock BluetoothGattCharacteristic
        characteristic.getValue() >> characteristicData['value']
        characteristic.getUuid() >> characteristicData['uuid']
        characteristic.getInstanceId() >> characteristicData['instanceId']
        characteristic
    }

    public mockDescriptorAndAttachToCharacteristic(BluetoothGattCharacteristic characteristic) {
        def descriptor = Spy(BluetoothGattDescriptor, constructorArgs: [NotificationAndIndicationManager.CLIENT_CHARACTERISTIC_CONFIG_UUID, 0])
        descriptor.getCharacteristic() >> characteristic
        characteristic.getDescriptor(NotificationAndIndicationManager.CLIENT_CHARACTERISTIC_CONFIG_UUID) >> descriptor
        descriptor
    }

    public shouldSetupCharacteristicNotificationCorrectly(UUID characteristicUUID, int instanceId) {
        def characteristic = mockCharacteristicWithValue(uuid: characteristicUUID, instanceId: instanceId, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        descriptorWriterMock.writeDescriptor(descriptor, _) >> Single.just(EMPTY_DATA)
        bluetoothGattMock.setCharacteristicNotification(characteristic, _) >> true
        characteristic
    }

}
