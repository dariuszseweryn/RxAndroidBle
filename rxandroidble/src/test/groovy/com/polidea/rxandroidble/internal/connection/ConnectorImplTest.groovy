package com.polidea.rxandroidble.internal.connection

import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble.ConnectionSetup
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.internal.serialization.ClientOperationQueue

import com.polidea.rxandroidble.internal.operations.ConnectOperation
import java.util.concurrent.atomic.AtomicReference
import rx.Observable
import rx.Subscription
import rx.observers.TestSubscriber
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import spock.lang.Specification
import spock.lang.Unroll

public class ConnectorImplTest extends Specification {

    ConnectionComponent.Builder mockConnectionComponentBuilder = Mock ConnectionComponent.Builder
    ConnectionComponent mockConnectionComponent = Mock ConnectionComponent
    PublishSubject disconnectErrorPublishSubject = PublishSubject.create()
    ClientOperationQueue clientOperationQueueMock = Mock ClientOperationQueue
    RxBleConnection mockConnection = Mock RxBleConnection
    RxBleGattCallback mockCallback = Mock RxBleGattCallback
    ConnectOperation mockConnect = Mock ConnectOperation
    TestSubscriber<RxBleConnection> testSubscriber = TestSubscriber.create()
    ConnectionSubscriptionWatcher mockConnectionSubscriptionAware0 = Mock ConnectionSubscriptionWatcher
    ConnectionSubscriptionWatcher mockConnectionSubscriptionAware1 = Mock ConnectionSubscriptionWatcher
    BluetoothGatt mockGatt = Mock BluetoothGatt
    ConnectionSetup defaultConnectionSetup = new ConnectionSetup.Builder().build()

    ConnectorImpl objectUnderTest

    def setup() {
        mockConnectionComponentBuilder.connectionModule(_) >> mockConnectionComponentBuilder
        mockConnectionComponentBuilder.build() >> mockConnectionComponent
        mockConnectionComponent.connectOperation() >> mockConnect
        mockConnectionComponent.gattCallback() >> mockCallback
        mockConnectionComponent.rxBleConnection() >> mockConnection
        mockConnectionComponent.connectionSubscriptionWatchers() >> new HashSet<>(Arrays.asList(mockConnectionSubscriptionAware0, mockConnectionSubscriptionAware1))
        mockCallback.observeDisconnect() >> disconnectErrorPublishSubject

        objectUnderTest = new ConnectorImpl(
                clientOperationQueueMock,
                mockConnectionComponentBuilder,
                Schedulers.immediate()
        )
    }

    @Unroll
    def "subscribing prepareConnection() should pass the provided ConnectionSetup in ConnectionModule to the ConnectionComponent.Builder"() {

        given:
        AtomicReference<ConnectionModule> connectionModuleAtomicReference = new AtomicReference<>()
        clientOperationQueueMock.queue(mockConnect) >> Observable.empty()
        def connectionSetup = new ConnectionSetup.Builder().setAutoConnect(autoConnect).setSuppressIllegalOperationCheck(suppressIllegalOperations).build()

        when:
        objectUnderTest.prepareConnection(connectionSetup).subscribe()

        then:
        1 * mockConnectionComponentBuilder.connectionModule({ ConnectionModule cm ->
            connectionModuleAtomicReference.set(cm)
            true
        }) >> mockConnectionComponentBuilder

        and:
        connectionModuleAtomicReference.get().autoConnect == autoConnect

        and:
        connectionModuleAtomicReference.get().suppressOperationCheck == suppressIllegalOperations

        where:
        [autoConnect, suppressIllegalOperations] << [[true, false], [true, false]].combinations()
    }

    def "should call ConnectionSubscriptionAware according to prepareConnection() subscription"() {

        given:
        clientOperationQueueMock.queue(mockConnect) >> Observable.empty()

        when:
        Subscription s = objectUnderTest.prepareConnection(defaultConnectionSetup).subscribe()

        then:
        1 * mockConnectionSubscriptionAware0.onConnectionSubscribed()
        1 * mockConnectionSubscriptionAware1.onConnectionSubscribed()

        when:
        s.unsubscribe()

        then:
        1 * mockConnectionSubscriptionAware0.onConnectionUnsubscribed()
        1 * mockConnectionSubscriptionAware1.onConnectionUnsubscribed()
    }

    def "should call ConnectionSubscriptionAware according to prepareConnection() subscription (error case)"() {

        given:
        PublishSubject connectPublishSubject = PublishSubject.create()
        clientOperationQueueMock.queue(mockConnect) >> connectPublishSubject

        when:
        objectUnderTest.prepareConnection(defaultConnectionSetup).subscribe(testSubscriber)

        then:
        1 * mockConnectionSubscriptionAware0.onConnectionSubscribed()
        1 * mockConnectionSubscriptionAware1.onConnectionSubscribed()

        when:
        connectPublishSubject.onError(new Throwable("test"))

        then:
        1 * mockConnectionSubscriptionAware0.onConnectionUnsubscribed()
        1 * mockConnectionSubscriptionAware1.onConnectionUnsubscribed()
    }

    def "subscribing prepareConnection() should schedule provided ConnectOperation on ClientOperationQueue"() {

        when:
        objectUnderTest.prepareConnection(defaultConnectionSetup).subscribe(testSubscriber)

        then:
        1 * clientOperationQueueMock.queue(mockConnect)
    }

    def "prepareConnection() should emit RxBleConnection and not complete"() {

        given:
        clientOperationQueueMock.queue(mockConnect) >> Observable.just(mockGatt)

        when:
        objectUnderTest.prepareConnection(defaultConnectionSetup).subscribe(testSubscriber)

        then:
        testSubscriber.assertValueCount(1)
        testSubscriber.assertNotCompleted()
    }

    def "prepareConnection() should emit error from RxBleGattCallback.disconnectedErrorObservable()"() {

        given:
        def testError = new Throwable("test")
        clientOperationQueueMock.queue(_) >> Observable.just(mockGatt)

        when:
        objectUnderTest.prepareConnection(defaultConnectionSetup).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(testError)
        mockCallback.observeDisconnect() >> Observable.error(testError) // Overwriting default behaviour
    }

    def "prepareConnection() should emit exception emitted by RxBleCallback.observeDisconnect()"() {

        given:
        RuntimeException testException = new RuntimeException("test")
        clientOperationQueueMock.queue(mockConnect) >> Observable.never()
        objectUnderTest.prepareConnection(defaultConnectionSetup).subscribe(testSubscriber)

        when:
        disconnectErrorPublishSubject.onError(testException)

        then:
        testSubscriber.assertError testException
    }

    def "prepareConnection() should emit exception emitted by RxBleCallback.observeDisconnect() even after connection"() {

        given:
        PublishSubject<BluetoothGatt> connectPublishSubject = PublishSubject.create()
        RuntimeException testException = new RuntimeException("test")
        clientOperationQueueMock.queue(mockConnect) >> connectPublishSubject
        objectUnderTest.prepareConnection(defaultConnectionSetup).subscribe(testSubscriber)
        connectPublishSubject.onNext(mockGatt)

        when:
        disconnectErrorPublishSubject.onError(testException)

        then:
        testSubscriber.assertError testException
    }

    def "should call ConnectionComponent.rxBleConnection() only after ConnectOperation will emit"() {

        given:
        PublishSubject<BluetoothGatt> connectPublishSubject = PublishSubject.create()
        clientOperationQueueMock.queue(mockConnect) >> connectPublishSubject

        when:
        objectUnderTest.prepareConnection(defaultConnectionSetup).subscribe(testSubscriber)

        then:
        0 * mockConnectionComponent.rxBleConnection()

        when:
        connectPublishSubject.onNext(mockGatt)

        then:
        1 * mockConnectionComponent.rxBleConnection() >> mockConnection
    }
}