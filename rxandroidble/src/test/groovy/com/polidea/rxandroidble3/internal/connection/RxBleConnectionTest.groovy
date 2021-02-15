package com.polidea.rxandroidble3.internal.connection

import android.bluetooth.*
import androidx.annotation.NonNull
import com.polidea.rxandroidble3.*
import com.polidea.rxandroidble3.exceptions.BleCharacteristicNotFoundException
import com.polidea.rxandroidble3.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble3.exceptions.BleGattOperationType
import com.polidea.rxandroidble3.internal.operations.OperationsProviderImpl
import com.polidea.rxandroidble3.internal.operations.ReadRssiOperation
import com.polidea.rxandroidble3.internal.util.ByteAssociation
import com.polidea.rxandroidble3.internal.util.MockOperationTimeoutConfiguration
import com.polidea.rxandroidble3.internal.logger.LoggerUtilBluetoothServices
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.functions.Predicate
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.TimeUnit

import static io.reactivex.rxjava3.core.Observable.just

class RxBleConnectionTest extends Specification {

    public static final CHARACTERISTIC_UUID = UUID.fromString("f301f518-5414-471c-8a7b-2ef6d1b7373d")
    public static final CHARACTERISTIC_INSTANCE_ID = 1
    public static final OTHER_UUID = UUID.fromString("ab906173-5daa-4d6b-8604-c2be69122d57")
    public static final OTHER_INSTANCE_ID = 2
    public static final byte[] NOT_EMPTY_DATA = [1, 2, 3] as byte[]
    public static final byte[] OTHER_DATA = [2, 2, 3] as byte[]
    public static final int EXPECTED_RSSI_VALUE = 5
    def dummyQueue = new DummyOperationQueue()
    def gattCallback = Mock RxBleGattCallback
    def bluetoothGattMock = Mock BluetoothGatt
    def mockServiceDiscoveryManager = Mock ServiceDiscoveryManager
    def illegalOperationChecker = Mock IllegalOperationChecker
    def testScheduler = new TestScheduler()
    def timeoutConfig = new MockOperationTimeoutConfiguration(testScheduler)
    def operationsProviderMock = new OperationsProviderImpl(gattCallback, bluetoothGattMock, Mock(LoggerUtilBluetoothServices),
            timeoutConfig, testScheduler, testScheduler,
            { new ReadRssiOperation(gattCallback, bluetoothGattMock, timeoutConfig) })
    def notificationAndIndicationManagerMock = Mock NotificationAndIndicationManager
    def descriptorWriterMock = Mock DescriptorWriter
    def mtuProvider = Mock MtuProvider
    def objectUnderTest = new RxBleConnectionImpl(dummyQueue, gattCallback, bluetoothGattMock, mockServiceDiscoveryManager,
            notificationAndIndicationManagerMock, mtuProvider, descriptorWriterMock, operationsProviderMock,
            { new LongWriteOperationBuilderImpl(dummyQueue, { 20 }, Mock(RxBleConnection)) }, testScheduler, illegalOperationChecker
    )
    def connectionStateChange = BehaviorSubject.create()

    def setup() {
        gattCallback.getOnConnectionStateChange() >> connectionStateChange
        illegalOperationChecker.checkAnyPropertyMatches(_, _) >> Completable.complete()
    }

    def "should proxy all calls to .discoverServices() to ServiceDiscoveryManager with proper timeouts"() {

        when:
        invokationClosure.call(objectUnderTest)

        then:
        1 * mockServiceDiscoveryManager.getDiscoverServicesSingle(timeout, timeoutTimeUnit) >> Single.just(new RxBleDeviceServices([]))

        where:
        timeout | timeoutTimeUnit  | invokationClosure
        20      | TimeUnit.SECONDS | { RxBleConnection objectUnderTest -> objectUnderTest.discoverServices() }
        2       | TimeUnit.HOURS   | { RxBleConnection objectUnderTest -> objectUnderTest.discoverServices(2, TimeUnit.HOURS) }
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
        def testSubscriber = setupWriteClosure.call(objectUnderTest, characteristic, OTHER_DATA).test()

        then:
        testSubscriber.assertError { BleGattCannotStartException e -> e.bleGattOperationType == BleGattOperationType.CHARACTERISTIC_WRITE }

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
        def testSubscriber = setupReadClosure.call(objectUnderTest, characteristic).test()

        then:
        testSubscriber.assertError { BleGattCannotStartException e -> e.bleGattOperationType == BleGattOperationType.CHARACTERISTIC_READ }

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
        def testSubscriber = objectUnderTest.readRssi().test()

        then:
        testSubscriber.assertError(BleGattCannotStartException)
        testSubscriber.assertErrorMessage("GATT exception from MAC address null, with type BleGattOperation{description='READ_RSSI'}")
        testSubscriber.assertError(new Predicate<BleGattCannotStartException>() {
            @Override
            boolean test(@io.reactivex.rxjava3.annotations.NonNull BleGattCannotStartException throwable) throws Exception {
                return throwable.bleGattOperationType == BleGattOperationType.READ_RSSI
            }
        })
    }

    def "should emit BleCharacteristicNotFoundException during read operation if no services were found"() {
        given:
        shouldDiscoverServices([])

        when:
        def testSubscriber = objectUnderTest.readCharacteristic(CHARACTERISTIC_UUID).test()

        then:
        testSubscriber.assertError BleCharacteristicNotFoundException
    }

    def "should emit BleCharacteristicNotFoundException during read operation if characteristic was not found"() {
        given:
        def service = Mock BluetoothGattService
        shouldDiscoverServices([service])
        service.getCharacteristic(_) >> null

        when:
        def testSubscriber = objectUnderTest.readCharacteristic(CHARACTERISTIC_UUID).test()

        then:
        testSubscriber.assertError { BleCharacteristicNotFoundException e -> e.charactersisticUUID == CHARACTERISTIC_UUID }
    }

    def "should read first found characteristic with matching UUID"() {
        given:
        def service = Mock BluetoothGattService
        shouldServiceContainCharacteristic(service, CHARACTERISTIC_UUID, CHARACTERISTIC_INSTANCE_ID, NOT_EMPTY_DATA)
        shouldServiceContainCharacteristic(service, OTHER_UUID, OTHER_INSTANCE_ID, OTHER_DATA)
        shouldDiscoverServices([service])
        shouldGattCallbackReturnDataOnRead(
                [uuid: OTHER_UUID, value: OTHER_DATA],
                [uuid: CHARACTERISTIC_UUID, value: NOT_EMPTY_DATA])

        when:
        def testSubscriber = objectUnderTest.readCharacteristic(CHARACTERISTIC_UUID).test()

        then:
        testSubscriber.assertValue NOT_EMPTY_DATA
    }

    def "should emit BleCharacteristicNotFoundException if there are no services during write operation"() {
        given:
        shouldDiscoverServices([])

        when:
        def testSubscriber = objectUnderTest.writeCharacteristic(CHARACTERISTIC_UUID, NOT_EMPTY_DATA).test()

        then:
        testSubscriber.assertError { BleCharacteristicNotFoundException e -> e.charactersisticUUID == CHARACTERISTIC_UUID }
    }

    def "should emit BleCharacteristicNotFoundException if characteristic was not found during write operation"() {
        given:
        shouldGattContainServiceWithCharacteristic(null)

        when:
        def testSubscriber = objectUnderTest.writeCharacteristic(CHARACTERISTIC_UUID, NOT_EMPTY_DATA).test()

        then:
        testSubscriber.assertError { BleCharacteristicNotFoundException e -> e.charactersisticUUID == CHARACTERISTIC_UUID }
    }

    @Unroll
    def "should write characteristic and return written value"() {
        given:
        def mockedCharacteristic = mockCharacteristicWithValue(uuid: CHARACTERISTIC_UUID, instanceId: CHARACTERISTIC_INSTANCE_ID, value: OTHER_DATA)
        shouldGattContainServiceWithCharacteristic(mockedCharacteristic, CHARACTERISTIC_UUID)
        def onWriteSubject = PublishSubject.create()
        gattCallback.getOnCharacteristicWrite() >> onWriteSubject

        when:
        def testSubscriber = setupWriteClosure.call(objectUnderTest, mockedCharacteristic, OTHER_DATA).test()

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
        def testSubscriber = objectUnderTest.readRssi().test()

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
        def testSubscriber = setupTriggerNotificationClosure.call(objectUnderTest, characteristic).flatMap({ it }).test()

        then:
        testSubscriber.assertError { BleCharacteristicNotFoundException e -> e.charactersisticUUID == CHARACTERISTIC_UUID }

        where:
        setupTriggerNotificationClosure << [
                { RxBleConnection connection, BluetoothGattCharacteristic aCharacteristic -> return connection.setupNotification(aCharacteristic.getUuid()) },
                { RxBleConnection connection, BluetoothGattCharacteristic aCharacteristic -> return connection.setupIndication(aCharacteristic.getUuid()) }
        ]
    }

    @Unroll
    def "should call NotificationAndIndicationManager when called by .setupNotification() / .setupIndication() properly"() {

        given:
        def characteristic = Mock BluetoothGattCharacteristic
        shouldGattContainServiceWithCharacteristic(characteristic)

        when:
        setupClosure.call(objectUnderTest, characteristic, mode)

        then:
        1 * notificationAndIndicationManagerMock.setupServerInitiatedCharacteristicRead(characteristic, mode, ack) >> Observable.empty()

        where:
        mode                          | ack   | setupClosure
        NotificationSetupMode.DEFAULT | false | { RxBleConnection con, BluetoothGattCharacteristic aChar, NotificationSetupMode nsm -> return con.setupNotification(aChar) }
        NotificationSetupMode.DEFAULT | false | { RxBleConnection con, BluetoothGattCharacteristic aChar, NotificationSetupMode nsm -> return con.setupNotification(aChar.getUuid()).subscribe() }
        NotificationSetupMode.DEFAULT | false | { RxBleConnection con, BluetoothGattCharacteristic aChar, NotificationSetupMode nsm -> return con.setupNotification(aChar, nsm) }
        NotificationSetupMode.DEFAULT | false | { RxBleConnection con, BluetoothGattCharacteristic aChar, NotificationSetupMode nsm -> return con.setupNotification(aChar.getUuid(), nsm).subscribe() }
        NotificationSetupMode.DEFAULT | true  | { RxBleConnection con, BluetoothGattCharacteristic aChar, NotificationSetupMode nsm -> return con.setupIndication(aChar) }
        NotificationSetupMode.DEFAULT | true  | { RxBleConnection con, BluetoothGattCharacteristic aChar, NotificationSetupMode nsm -> return con.setupIndication(aChar.getUuid()).subscribe() }
        NotificationSetupMode.DEFAULT | true  | { RxBleConnection con, BluetoothGattCharacteristic aChar, NotificationSetupMode nsm -> return con.setupIndication(aChar, nsm) }
        NotificationSetupMode.DEFAULT | true  | { RxBleConnection con, BluetoothGattCharacteristic aChar, NotificationSetupMode nsm -> return con.setupIndication(aChar.getUuid(), nsm).subscribe() }
        NotificationSetupMode.COMPAT  | false | { RxBleConnection con, BluetoothGattCharacteristic aChar, NotificationSetupMode nsm -> return con.setupNotification(aChar, nsm) }
        NotificationSetupMode.COMPAT  | false | { RxBleConnection con, BluetoothGattCharacteristic aChar, NotificationSetupMode nsm -> return con.setupNotification(aChar.getUuid(), nsm).subscribe() }
        NotificationSetupMode.COMPAT  | true  | { RxBleConnection con, BluetoothGattCharacteristic aChar, NotificationSetupMode nsm -> return con.setupIndication(aChar, nsm) }
        NotificationSetupMode.COMPAT  | true  | { RxBleConnection con, BluetoothGattCharacteristic aChar, NotificationSetupMode nsm -> return con.setupIndication(aChar.getUuid(), nsm).subscribe() }
    }

    def "should proxy .getMtu() calls to MtuProvider"() {

        given:
        int mtuValue = 10

        when:
        int receivedMtuValue = objectUnderTest.getMtu()

        then:
        1 * mtuProvider.getMtu() >> mtuValue

        and:
        receivedMtuValue == mtuValue
    }

    def "should pass items emitted by RxBleGattCallback.getConnectionParametersUpdates()"() {
        given:
        def connectionParametersPublishSubject = PublishSubject.create()
        gattCallback.getConnectionParametersUpdates() >> connectionParametersPublishSubject
        def mockConnectionParameters = Mock ConnectionParameters
        def testSubscriber = objectUnderTest.observeConnectionParametersUpdates().test()

        when:
        connectionParametersPublishSubject.onNext(mockConnectionParameters)

        then:
        testSubscriber.assertValue(mockConnectionParameters)
    }

    def "should pass items emitted by observable returned from RxBleCustomOperation.asObservable()"() {
        given:
        def customOperation = customOperationWithOutcome {
            just(true, false, true)
        }

        when:
        def testSubscriber = objectUnderTest.queue(customOperation).test()

        then:
        testSubscriber.assertValues(true, false, true)
    }

    def "should pass error if custom operation will throw out of RxBleCustomOperation.asObservable()"() {
        given:
        def customOperation = customOperationWithOutcome { throw new RuntimeException() }

        when:
        def testSubscriber = objectUnderTest.queue(customOperation).test()

        then:
        testSubscriber.assertError(RuntimeException.class)
    }

    def "should release the queue if custom operation will throw out of RxBleCustomOperation.asObservable()"() {
        given:
        def customOperation = customOperationWithOutcome { throw new RuntimeException() }

        when:
        def testSubscriber = objectUnderTest.queue(customOperation).test()

        then:
        dummyQueue.semaphore.isReleased()
    }

    def "should pass error if observable returned from RxBleCustomOperation.asObservable() will emit error"() {
        given:
        def customOperation = customOperationWithOutcome { Observable.error(new RuntimeException()) }

        when:
        def testSubscriber = objectUnderTest.queue(customOperation).test()

        then:
        testSubscriber.assertError(RuntimeException.class)
    }

    def "should clear native gatt callback after custom operation is finished"() {
        given:
        def nativeCallback = Mock BluetoothGattCallback
        def customOperation = new RxBleCustomOperation<Boolean>() {

            @Override
            Observable<byte[]> asObservable(BluetoothGatt bluetoothGatt, RxBleGattCallback rxBleGattCallback,
                                            Scheduler scheduler) throws Throwable {
                rxBleGattCallback.setNativeCallback(nativeCallback)
                return just(true)
            }
        }

        when:
        objectUnderTest.queue(customOperation).test()

        then:
        1 * gattCallback.setNativeCallback(null)
    }

    def "should clear native gatt callback after custom operation failed"() {
        given:
        def nativeCallback = Mock BluetoothGattCallback
        def customOperation = new RxBleCustomOperation<Boolean>() {

            @NonNull
            @Override
            Observable<byte[]> asObservable(BluetoothGatt bluetoothGatt,
                                            RxBleGattCallback rxBleGattCallback,
                                            Scheduler scheduler) throws Throwable {
                rxBleGattCallback.setNativeCallback(nativeCallback)
                return Observable.error(new IllegalArgumentException("Oh no, da error!"))
            }
        }

        when:
        objectUnderTest.queue(customOperation).test()

        then:
        1 * gattCallback.setNativeCallback(null)
    }

    def "should clear hidden native gatt callback after custom operation is finished"() {
        given:
        def hiddenNativeCallback = Mock HiddenBluetoothGattCallback
        def customOperation = new RxBleCustomOperation<Boolean>() {

            @Override
            Observable<byte[]> asObservable(BluetoothGatt bluetoothGatt, RxBleGattCallback rxBleGattCallback,
                                            Scheduler scheduler) throws Throwable {
                rxBleGattCallback.setHiddenNativeCallback(hiddenNativeCallback)
                return just(true)
            }
        }

        when:
        objectUnderTest.queue(customOperation).test()

        then:
        1 * gattCallback.setHiddenNativeCallback(null)
    }

    def "should clear hidden native gatt callback after custom operation failed"() {
        given:
        def hiddenNativeCallback = Mock HiddenBluetoothGattCallback
        def customOperation = new RxBleCustomOperation<Boolean>() {

            @NonNull
            @Override
            Observable<byte[]> asObservable(BluetoothGatt bluetoothGatt,
                                            RxBleGattCallback rxBleGattCallback,
                                            Scheduler scheduler) throws Throwable {
                rxBleGattCallback.setHiddenNativeCallback(hiddenNativeCallback)
                return Observable.error(new IllegalArgumentException("Oh no, da error!"))
            }
        }

        when:
        objectUnderTest.queue(customOperation).test()

        then:
        1 * gattCallback.setHiddenNativeCallback(null)
    }

    def "should release the queue if observable returned from RxBleCustomOperation.asObservable() will emit error"() {
        given:
        def customOperation = customOperationWithOutcome { Observable.error(new RuntimeException()) }

        when:
        objectUnderTest.queue(customOperation).test()

        then:
        dummyQueue.semaphore.isReleased()
    }

    def "should pass completion to subscriber when observable returned from RxBleCustomOperation.asObservable() will complete"() {
        given:
        def customOperation = customOperationWithOutcome { Observable.empty() }

        when:
        def testSubscriber = objectUnderTest.queue(customOperation).test()

        then:
        testSubscriber.assertComplete()
    }

    def "should release the queue when observable returned from RxBleCustomOperation.asObservable() will complete"() {
        given:
        def customOperation = customOperationWithOutcome { Observable.empty() }

        when:
        objectUnderTest.queue(customOperation).test()

        then:
        dummyQueue.semaphore.isReleased()
    }

    def "should throw illegal argument exception if RxBleCustomOperation.asObservable() return null"() {
        given:
        def customOperation = customOperationWithOutcome { null }

        when:
        def testSubscriber = objectUnderTest.queue(customOperation).test()

        then:
        testSubscriber.assertError(IllegalArgumentException.class)
    }

    def "should release queue if RxBleCustomOperation.asObservable() return null"() {
        given:
        def customOperation = customOperationWithOutcome { null }

        when:
        objectUnderTest.queue(customOperation).test()

        then:
        dummyQueue.semaphore.isReleased()
    }

    @Unroll
    def "should release the queue when observable returned from RxBleCustomOperation.asObservable() will terminate even if was unsubscribed before"() {

        given:
        def publishSubject = PublishSubject.create()
        def customOperation = customOperationWithOutcome { publishSubject }
        def testSubscriber = objectUnderTest.queue(customOperation).test()

        when:
        testSubscriber.dispose()

        then:
        !dummyQueue.semaphore.isReleased()

        when:
        callback.call(publishSubject)

        then:
        dummyQueue.semaphore.isReleased()

        where:
        callback << [
                { PublishSubject o -> o.onComplete() },
                { PublishSubject o -> o.onError(new Throwable()) }
        ]
    }

    def customOperationWithOutcome(Closure<Observable<Boolean>> outcomeSupplier) {
        new RxBleCustomOperation<Boolean>() {

            @NonNull
            @Override
            Observable<byte[]> asObservable(BluetoothGatt bluetoothGatt,
                                            RxBleGattCallback rxBleGattCallback,
                                            Scheduler scheduler) throws Throwable {
                outcomeSupplier()
            }
        }
    }

    def mockDescriptorAndAttachToCharacteristic(BluetoothGattCharacteristic characteristic) {
        def descriptor = Spy(BluetoothGattDescriptor, constructorArgs: [RxBleConnectionImpl.CLIENT_CHARACTERISTIC_CONFIG_UUID, 0])
        descriptor.getCharacteristic() >> characteristic
        characteristic.getDescriptor(RxBleConnectionImpl.CLIENT_CHARACTERISTIC_CONFIG_UUID) >> descriptor
        descriptor
    }

    def shouldGattContainServiceWithCharacteristic(BluetoothGattCharacteristic characteristic, UUID characteristicUUID = CHARACTERISTIC_UUID) {
        characteristic.getUuid() >> characteristicUUID
        shouldContainOneServiceWithoutCharacteristics().getCharacteristic(characteristicUUID) >> characteristic
    }

    def shouldContainOneServiceWithoutCharacteristics() {
        def service = Mock BluetoothGattService
        shouldDiscoverServices([service])
        service
    }

    def shouldReturnStartingStatusAndEmitRssiValueThroughCallback(Closure<Boolean> closure) {
        def rssiSubject = PublishSubject.create()
        gattCallback.getOnRssiRead() >> rssiSubject
        bluetoothGattMock.readRemoteRssi() >> { closure?.call(rssiSubject) }
    }

    def shouldServiceContainCharacteristic(BluetoothGattService service, UUID uuid, int instanceId, byte[] characteristicValue) {
        service.getCharacteristic(uuid) >> mockCharacteristicWithValue(uuid: uuid, instanceId: instanceId, value: characteristicValue)
    }

    def shouldGattCallbackReturnDataOnRead(Map... parameters) {
        gattCallback.getOnCharacteristicRead() >> {
            Observable.fromIterable(parameters.collect { ByteAssociation.create it['uuid'], it['value'] }) }
    }

    def mockCharacteristicWithValue(Map characteristicData) {
        def characteristic = Mock BluetoothGattCharacteristic
        characteristic.getValue() >> characteristicData['value']
        characteristic.getUuid() >> characteristicData['uuid']
        characteristic.getInstanceId() >> characteristicData['instanceId']
        characteristic
    }

    def shouldDiscoverServices(ArrayList<BluetoothGattService> services) {
        mockServiceDiscoveryManager.getDiscoverServicesSingle(_, _) >> Single.just(new RxBleDeviceServices(services))
    }

    def shouldFailStartingCharacteristicWrite() {
        bluetoothGattMock.writeCharacteristic(_) >> false
    }

    def shouldFailStartingCharacteristicRead() {
        bluetoothGattMock.readCharacteristic(_) >> false
    }

    private static Closure<Observable<byte[]>> readCharacteristicUuidClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic -> return connection.readCharacteristic(characteristic.getUuid()) }

    private static Closure<Observable<byte[]>> readCharacteristicCharacteristicClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic -> return connection.readCharacteristic(characteristic) }

    private static Closure<Observable<byte[]>> writeCharacteristicUuidClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic, byte[] data -> return connection.writeCharacteristic(characteristic.getUuid(), data) }

    private static Closure<Observable<byte[]>> writeCharacteristicCharacteristicClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic, byte[] data -> return connection.writeCharacteristic(characteristic, data) }

    @SuppressWarnings("GrDeprecatedAPIUsage")
    private static Closure<Observable<byte[]>> writeCharacteristicCharacteristicDeprecatedClosure = { RxBleConnection connection, BluetoothGattCharacteristic characteristic, byte[] data ->
        characteristic.setValue(data)
        return connection.writeCharacteristic(characteristic)
    }
}
