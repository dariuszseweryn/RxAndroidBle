package com.polidea.rxandroidble2.internal.operations

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattService
import com.polidea.rxandroidble2.RxBleDeviceServices
import com.polidea.rxandroidble2.exceptions.BleGattCallbackTimeoutException
import com.polidea.rxandroidble2.exceptions.BleGattCannotStartException
import com.polidea.rxandroidble2.exceptions.BleGattOperationType
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface
import com.polidea.rxandroidble2.internal.util.MockOperationTimeoutConfiguration
import com.polidea.rxandroidble2.internal.logger.LoggerUtilBluetoothServices
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import spock.lang.Specification

import java.util.concurrent.TimeUnit

public class OperationServicesDiscoverTest extends Specification {

    static long timeout = 20
    static TimeUnit timeoutTimeUnit = TimeUnit.SECONDS
    QueueReleaseInterface mockQueueReleaseInterface = Mock QueueReleaseInterface
    LoggerUtilBluetoothServices mockRxBleServicesLogger = Mock LoggerUtilBluetoothServices
    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt
    RxBleGattCallback mockGattCallback = Mock RxBleGattCallback
    TestScheduler testScheduler = new TestScheduler()
    PublishSubject<RxBleDeviceServices> onServicesDiscoveredPublishSubject = PublishSubject.create()
    ServiceDiscoveryOperation objectUnderTest

    def setup() {
        mockGattCallback.getOnServicesDiscovered() >> onServicesDiscoveredPublishSubject
        prepareObjectUnderTest()
    }

    def "sdox"() {
        expect:
        PublishSubject<Boolean> subject = PublishSubject.create()
        def disposable = Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            void subscribe(@NonNull ObservableEmitter emitter) throws Exception {
//                subject.subscribe(DisposableUtil.disposableObserverFromEmitter(emitter))
                emitter.onNext(true)
                emitter.onComplete()
//                emitter.onError(new RuntimeException("Aha!"))
            }
        })
        .firstOrError()
                .test()
        disposable.dispose()
//        subject.onError(new RuntimeException("Aha!"))
        disposable.assertNoErrors()
    }

    def "should call BluetoothGatt.discoverServices() exactly once when run()"() {

        when:
        objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        1 * mockBluetoothGatt.discoverServices() >> true
    }

    def "should emit an error if BluetoothGatt.discoverServices() returns false"() {

        given:
        mockBluetoothGatt.discoverServices() >> false

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertError BleGattCannotStartException

        and:
        testSubscriber.assertError {
            it.getBleGattOperationType() == BleGattOperationType.SERVICE_DISCOVERY
        }

        and:
        1 * mockQueueReleaseInterface.release()
    }

    def "should emit an error if RxBleGattCallback will emit error on RxBleGattCallback.getOnServicesDiscovered() and release queue"() {
        given:
        mockBluetoothGatt.discoverServices() >> true
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()
        def testException = new Exception("test")

        when:
        onServicesDiscoveredPublishSubject.onError(testException)

        then:
        testSubscriber.assertError(testException)

        and:
        (1.._) * mockQueueReleaseInterface.release() // technically it's not an error to call it more than once
    }

    def "should emit exactly one value when RxBleGattCallback.getOnServicesDiscovered() emits value"() {

        given:
        def value1 = new RxBleDeviceServices(new ArrayList<>())
        def value2 = new RxBleDeviceServices(new ArrayList<>())
        def value3 = new RxBleDeviceServices(new ArrayList<>())
        mockBluetoothGatt.discoverServices() >> true
        onServicesDiscoveredPublishSubject.onNext(value1)

        when:
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        then:
        testSubscriber.assertNoValues()

        when:
        onServicesDiscoveredPublishSubject.onNext(value2)

        then:
        testSubscriber.assertValue(value2)

        and:
        1 * mockQueueReleaseInterface.release()

        when:
        onServicesDiscoveredPublishSubject.onNext(value3)

        then:
        testSubscriber.assertValueCount(1) // no more values
    }

    def "should timeout after specified amount of time if BluetoothGatt.getServices() will return empty list"() {

        given:
        mockBluetoothGatt.discoverServices() >> true
        mockGattCallback.onServicesDiscovered >> Observable.never()
        mockBluetoothGatt.getServices() >> []
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        testScheduler.advanceTimeTo(timeout, timeoutTimeUnit)

        then:
        testSubscriber.assertError(BleGattCallbackTimeoutException)

        and:
        testSubscriber.assertError {
            ((BleGattCallbackTimeoutException) it).getBleGattOperationType() == BleGattOperationType.SERVICE_DISCOVERY
        }
    }

    def "should not timeout after specified amount of time if BluetoothGatt.getServices() will return non-empty list"() {

        given:
        mockBluetoothGatt.discoverServices() >> true
        mockGattCallback.onServicesDiscovered >> Observable.never()
        mockBluetoothGatt.getServices() >> createMockedBluetoothGattServiceList()
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        testScheduler.advanceTimeTo(timeout, timeoutTimeUnit)

        then:
        testSubscriber.assertNoErrors()
    }

    def "should emit valid RxBleServices after specified timeout was reached if BluetoothGatt won't get onDiscoveryCompleted() callback (therefore no RxBleGattCallback.onServicesDiscovered() emission) if BluetoothGatt.getServices() will return not-empty list - should wait before emitting 5 more seconds"() {

        given:
        mockBluetoothGatt.discoverServices() >> true
        mockGattCallback.onServicesDiscovered >> Observable.never()
        def mockedBluetoothGattServiceList = createMockedBluetoothGattServiceList()
        mockBluetoothGatt.getServices() >> mockedBluetoothGattServiceList
        def testSubscriber = objectUnderTest.run(mockQueueReleaseInterface).test()

        when:
        testScheduler.advanceTimeTo(timeout + 5, timeoutTimeUnit)

        then:
        testSubscriber.assertAnyOnNext { RxBleDeviceServices services ->
            services.bluetoothGattServices.containsAll(mockedBluetoothGattServiceList)
        }
    }

    private prepareObjectUnderTest() {
        objectUnderTest = new ServiceDiscoveryOperation(mockGattCallback, mockBluetoothGatt, mockRxBleServicesLogger,
                new MockOperationTimeoutConfiguration(timeout.toInteger(), testScheduler))
    }

    private List<BluetoothGattService> createMockedBluetoothGattServiceList() {
        return [Mock(BluetoothGattService), Mock(BluetoothGattService)]
    }
}