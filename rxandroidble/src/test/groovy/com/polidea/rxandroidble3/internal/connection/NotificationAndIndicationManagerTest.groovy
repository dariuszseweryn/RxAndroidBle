package com.polidea.rxandroidble3.internal.connection

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.polidea.rxandroidble3.NotificationSetupMode
import com.polidea.rxandroidble3.exceptions.BleCannotSetCharacteristicNotificationException
import com.polidea.rxandroidble3.exceptions.BleConflictingNotificationAlreadySetException
import com.polidea.rxandroidble3.internal.util.CharacteristicChangedEvent
import hkhc.electricspock.ElectricSpecification
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import org.robolectric.annotation.Config
import spock.lang.Unroll

import static io.reactivex.rxjava3.core.Observable.just

@Config(manifest = Config.NONE)
class NotificationAndIndicationManagerTest extends ElectricSpecification {

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
    public static final NotificationSetupMode[] ALL_MODES = NotificationSetupMode.values()
    public static final NotificationSetupMode[] NON_COMPAT_MODES = [NotificationSetupMode.DEFAULT, NotificationSetupMode.QUICK_SETUP]
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
        descriptorWriterMock.writeDescriptor(_, _) >> Completable.complete()
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
        descriptorWriterMock.writeDescriptor(_, _) >> Completable.complete()
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
    def "should emit Observable<byte[]> before DescriptorWriter.writeDescriptor() emits when in QUICK_SETUP mode"() {

        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        descriptorWriterMock.writeDescriptor(_, _) >> Completable.never()
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.never()
        mockDescriptorAndAttachToCharacteristic(characteristic)
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true

        when:
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, NotificationSetupMode.QUICK_SETUP, ack).test()

        then:
        testSubscriber.assertValueCount(1)

        where:
        ack << ACK_VALUES
    }

    @Unroll
    def "should emit BleCannotSetCharacteristicNotificationException with CANNOT_SET_LOCAL_NOTIFICATION reason if failed to set characteristic notification ack:#ack mode:#mode"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        descriptorWriterMock.writeDescriptor(_, _) >> Completable.complete()
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
        [ack, mode] << [ACK_VALUES, ALL_MODES].combinations()
    }

    @Unroll
    def "should emit BleCannotSetCharacteristicNotificationException with CANNOT_WRITE_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR reason and a cause if failed to write successfully CCC Descriptor when in DEFAULT mode ack:#ack"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.empty()
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        def testExceptionCause = new RuntimeException()
        descriptorWriterMock.writeDescriptor(descriptor, _) >> Completable.error(testExceptionCause)

        when:
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, NotificationSetupMode.DEFAULT, ack).test()

        then:
        testSubscriber.assertError {
            Throwable e ->
                e instanceof BleCannotSetCharacteristicNotificationException &&
                e.getReason() == BleCannotSetCharacteristicNotificationException.CANNOT_WRITE_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR &&
                        e.getCause() == testExceptionCause
        }

        where:
        ack << ACK_VALUES
    }

    @Unroll
    def "should emit BleCannotSetCharacteristicNotificationException with CANNOT_WRITE_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR reason and a cause if failed to write successfully CCC Descriptor (from the emitted Observable<byte>) when in QUICK_SETUP mode ack:#ack"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.never()
        PublishSubject<byte[]> descriptorWriteResult = PublishSubject.create()
        descriptorWriterMock.writeDescriptor(descriptor, _) >> descriptorWriteResult.ignoreElements()
        def parentTestObserver = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, NotificationSetupMode.QUICK_SETUP, ack).test()
        def notificationObservable = parentTestObserver.values().get(0)
        notificationObservable.test()
        def testExceptionCause = new RuntimeException("test")

        when:
        descriptorWriteResult.onError(testExceptionCause)

        then:
        parentTestObserver.assertError {
            Throwable e ->
                e instanceof BleCannotSetCharacteristicNotificationException &&
                        e.getReason() == BleCannotSetCharacteristicNotificationException.CANNOT_WRITE_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR &&
                        e.getCause() == testExceptionCause
        }

        where:
        ack << ACK_VALUES
    }

    @Unroll
    def "should complete the emitted io.reactivex.rxjava3.core.Observable<byte> when an error happens while writing CCC in QUICK_SETUP mode ack:#ack"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.never()
        PublishSubject<byte[]> descriptorWriteResult = PublishSubject.create()
        descriptorWriterMock.writeDescriptor(descriptor, _) >> descriptorWriteResult.ignoreElements()
        def parentTestObserver = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, NotificationSetupMode.QUICK_SETUP, ack).test()
        def notificationObservable = parentTestObserver.values().get(0)
        def childTestObserver = notificationObservable.test()
        def testExceptionCause = new RuntimeException("test")

        when:
        descriptorWriteResult.onError(testExceptionCause)

        then:
        childTestObserver.assertComplete()

        where:
        ack << ACK_VALUES
    }

    @Unroll
    def "should subscribe to DescriptorWriter.writeDescriptor() only after subscription to the emitted io.reactivex.rxjava3.core.Observable<byte[]> is made in QUICK_SETUP mode ack:#ack"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.never()
        PublishSubject<byte[]> descriptorWriteResult = PublishSubject.create()
        descriptorWriterMock.writeDescriptor(descriptor, _) >> descriptorWriteResult.ignoreElements()
        def parentTestObserver = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, NotificationSetupMode.QUICK_SETUP, ack).test()
        def notificationObservable = parentTestObserver.values().get(0)

        expect:
        !descriptorWriteResult.hasObservers()

        when:
        notificationObservable.subscribe()

        then:
        descriptorWriteResult.hasObservers()

        where:
        ack << ACK_VALUES
    }

    @Unroll
    def "should not subscribe to DescriptorWriter.writeDescriptor() after subscription to the parent io.reactivex.rxjava3.core.Observable<Observable<byte[]>> was unsubscribed in QUICK_SETUP mode ack:#ack"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.never()
        PublishSubject<byte[]> descriptorWriteResult = PublishSubject.create()
        descriptorWriterMock.writeDescriptor(descriptor, _) >> descriptorWriteResult.ignoreElements()
        def parentTestObserver = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, NotificationSetupMode.QUICK_SETUP, ack).test()
        def notificationObservable = parentTestObserver.values().get(0)
        parentTestObserver.dispose()

        when:
        notificationObservable.subscribe()

        then:
        !descriptorWriteResult.hasObservers()

        where:
        ack << ACK_VALUES
    }

    @Unroll
    def "should not subscribe to DescriptorWriter.writeDescriptor() twice in QUICK_SETUP mode when more than one subscription is made to the child io.reactivex.rxjava3.core.Observable<byte[]> ack:#ack"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.never()
        PublishSubject<byte[]> descriptorWriteResult = PublishSubject.create()
        Completable descriptorWriteCompletable = descriptorWriteResult.publish().autoConnect(2).ignoreElements()
        descriptorWriterMock.writeDescriptor(descriptor, _) >> descriptorWriteCompletable
        def parentTestObserver = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, NotificationSetupMode.QUICK_SETUP, ack).test()
        def notificationObservable = parentTestObserver.values().get(0)

        when:
        notificationObservable.subscribe()
        notificationObservable.subscribe()

        then:
        !descriptorWriteResult.hasObservers()

        where:
        ack << ACK_VALUES
    }

    @Unroll
    def "should not subscribe again to DescriptorWriter.writeDescriptor() if first subscription finished with '#result' in QUICK_SETUP mode ack:#ack"() {
        given:
        def completableForResultGetter = { if (it == "complete") Completable.complete() else Completable.error(new RuntimeException("Test")) }
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.never()
        PublishSubject<Completable> descriptorWriteResult = PublishSubject.create()
        descriptorWriterMock.writeDescriptor(descriptor, _) >> descriptorWriteResult.take(1).flatMapCompletable({ it })
        def parentTestObserver = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, NotificationSetupMode.QUICK_SETUP, ack).test()
        def notificationObservable = parentTestObserver.values().get(0)
        def disposable = notificationObservable.subscribe()
        descriptorWriteResult.onNext(completableForResultGetter(result))
        disposable.dispose()

        when:
        notificationObservable.subscribe()

        then:
        !descriptorWriteResult.hasObservers()

        where:
        [ack, result] << [ACK_VALUES, ["complete", "error"]].combinations()
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
        [mode, ack] << [ALL_MODES, ACK_VALUES].combinations()
    }

    @Unroll
    def "should proxy RxBleGattCallback.observeDisconnect() if happened after io.reactivex.rxjava3.core.Observable<byte[]> emission"() {
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
        [mode, ack] << [ALL_MODES, ACK_VALUES].combinations()
    }

    @Unroll
    def "should write proper value to CCC Descriptor when in non COMPAT mode mode:#mode ack:#ack"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.never()
        descriptorWriterMock.writeDescriptor(descriptor, DISABLE_NOTIFICATION_VALUE) >> Completable.complete()

        when:
        objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack).test()

        then:
        1 * descriptorWriterMock.writeDescriptor(descriptor, enableValueForAck(ack)) >> Completable.complete() // TODO delayed!

        where:
        [mode, ack] << [NON_COMPAT_MODES, ACK_VALUES].combinations()
    }

    @Unroll
    def "should notify about value change and stay subscribed"() {
        given:
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
                ALL_MODES,
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
                ALL_MODES,
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
        descriptorWriterMock.writeDescriptor(descriptor, _) >> Completable.complete()

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
        [mode, ack] << [ALL_MODES, ACK_VALUES].combinations()
    }

    @Unroll
    def "should not setup another notification if one was already done on the same characteristic even if not subscribed yet"() {
        given:
        def characteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        bluetoothGattMock.setCharacteristicNotification(characteristic, true) >> true
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> PublishSubject.create()
        descriptorWriterMock.writeDescriptor(descriptor, _) >> Completable.complete()
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
        [mode, ack] << [ALL_MODES, ACK_VALUES].combinations()
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
        [mode, ack] << [ALL_MODES, ACK_VALUES].combinations()
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
        writerCalls * descriptorWriterMock.writeDescriptor(descriptor, { it == enableValueForAck(ack) }) >> Completable.complete()

        when:
        firstSubscription.dispose()

        then:
        0 * bluetoothGattMock.setCharacteristicNotification(characteristic, false) >> true
        0 * descriptorWriterMock.writeDescriptor(descriptor, _) >> Completable.complete()

        when:
        secondSubscription.dispose()

        then:
        1 * bluetoothGattMock.setCharacteristicNotification(characteristic, false) >> true
        writerCalls * descriptorWriterMock.writeDescriptor(descriptor, { it == DISABLE_NOTIFICATION_VALUE }) >> Completable.complete()

        where:
        mode                              | ack   | writerCalls
        NotificationSetupMode.DEFAULT     | true  | 1
        NotificationSetupMode.DEFAULT     | false | 1
        NotificationSetupMode.COMPAT      | true  | 0
        NotificationSetupMode.COMPAT      | false | 0
        NotificationSetupMode.QUICK_SETUP | true  | 1
        NotificationSetupMode.QUICK_SETUP | false | 1
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
                ALL_MODES,
                ALL_MODES,
                [[true, false], [false, true]]
        ].combinations()
    }

    @Unroll
    def "should complete the emitted io.reactivex.rxjava3.core.Observable<byte> when unsubscribed"() {
        given:
        def characteristic = shouldSetupCharacteristicNotificationCorrectly(CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID)
        rxBleGattCallbackMock.getOnCharacteristicChanged() >> Observable.never()
        def testSubscriber = objectUnderTest.setupServerInitiatedCharacteristicRead(characteristic, mode, ack).test()
        def emittedObservableSubscriber = testSubscriber.values().get(0).test()

        when:
        testSubscriber.dispose()

        then:
        emittedObservableSubscriber.assertComplete()

        where:
        [mode, ack] << [ALL_MODES, ACK_VALUES].combinations()
    }

    @Unroll
    def "should proxy the error emitted by RxBleGattCallback.getOnCharacteristicChanged() to emitted io.reactivex.rxjava3.core.Observable<byte>"() {
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
        [mode, ack] << [ALL_MODES, ACK_VALUES].combinations()
    }

    def mockCharacteristicWithValue(Map characteristicData) {
        def characteristic = Mock BluetoothGattCharacteristic
        characteristic.getValue() >> characteristicData['value']
        characteristic.getUuid() >> characteristicData['uuid']
        characteristic.getInstanceId() >> characteristicData['instanceId']
        characteristic
    }

    def mockDescriptorAndAttachToCharacteristic(BluetoothGattCharacteristic characteristic) {
        def descriptor = Spy(BluetoothGattDescriptor, constructorArgs: [NotificationAndIndicationManager.CLIENT_CHARACTERISTIC_CONFIG_UUID, 0])
        descriptor.getCharacteristic() >> characteristic
        characteristic.getDescriptor(NotificationAndIndicationManager.CLIENT_CHARACTERISTIC_CONFIG_UUID) >> descriptor
        descriptor
    }

    def shouldSetupCharacteristicNotificationCorrectly(UUID characteristicUUID, int instanceId) {
        def characteristic = mockCharacteristicWithValue(uuid: characteristicUUID, instanceId: instanceId, value: EMPTY_DATA)
        def descriptor = mockDescriptorAndAttachToCharacteristic(characteristic)
        descriptorWriterMock.writeDescriptor(descriptor, _) >> Completable.complete()
        bluetoothGattMock.setCharacteristicNotification(characteristic, _) >> true
        characteristic
    }

    def enableValueForAck(boolean ack) {
        return ack ? ENABLE_INDICATION_VALUE : ENABLE_NOTIFICATION_VALUE;
    }
}
