package com.polidea.rxandroidble2.internal.connection

import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble2.ConnectionSetup
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.Timeout
import com.polidea.rxandroidble2.internal.operations.ConnectOperation
import com.polidea.rxandroidble2.internal.serialization.ClientOperationQueue
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import spock.lang.Specification
import spock.lang.Unroll

class ConnectorImplTest extends Specification {

    ConnectionComponent.Builder mockConnectionComponentBuilder = Mock ConnectionComponent.Builder
    ConnectionComponent mockConnectionComponent = Mock ConnectionComponent
    PublishSubject disconnectErrorPublishSubject = PublishSubject.create()
    ClientOperationQueue clientOperationQueueMock = Mock ClientOperationQueue
    RxBleConnection mockConnection = Mock RxBleConnection
    RxBleGattCallback mockCallback = Mock RxBleGattCallback
    ConnectOperation mockConnect = Mock ConnectOperation
    ConnectionSubscriptionWatcher mockConnectionSubscriptionAware0 = Mock ConnectionSubscriptionWatcher
    ConnectionSubscriptionWatcher mockConnectionSubscriptionAware1 = Mock ConnectionSubscriptionWatcher
    BluetoothGatt mockGatt = Mock BluetoothGatt
    ConnectionSetup defaultConnectionSetup = new ConnectionSetup.Builder().build()

    ConnectorImpl objectUnderTest

    def setup() {
        mockConnectionComponentBuilder.autoConnect(_) >> mockConnectionComponentBuilder
        mockConnectionComponentBuilder.suppressOperationChecks(_) >> mockConnectionComponentBuilder
        mockConnectionComponentBuilder.operationTimeout(_) >> mockConnectionComponentBuilder
        mockConnectionComponentBuilder.build() >> mockConnectionComponent
        mockConnectionComponent.connectOperation() >> mockConnect
        mockConnectionComponent.gattCallback() >> mockCallback
        mockConnectionComponent.rxBleConnection() >> mockConnection
        mockConnectionComponent.connectionSubscriptionWatchers() >> new HashSet<>(Arrays.asList(mockConnectionSubscriptionAware0, mockConnectionSubscriptionAware1))
        mockCallback.observeDisconnect() >> disconnectErrorPublishSubject

        objectUnderTest = new ConnectorImpl(
                clientOperationQueueMock,
                mockConnectionComponentBuilder,
                Schedulers.trampoline()
        )
    }

    @Unroll
    def "subscribing prepareConnection() should pass the provided ConnectionSetup in ConnectionModule to the ConnectionComponent.Builder"() {

        given:
        clientOperationQueueMock.queue(mockConnect) >> Observable.empty()
        def operationTimeout = Mock(Timeout)
        def connectionSetup = new ConnectionSetup.Builder()
                .setAutoConnect(autoConnect)
                .setSuppressIllegalOperationCheck(suppressIllegalOperations)
                .setOperationTimeout(operationTimeout)
                .build()

        when:
        objectUnderTest.prepareConnection(connectionSetup).subscribe()

        then:
        1 * mockConnectionComponentBuilder.autoConnect(autoConnect) >> mockConnectionComponentBuilder

        and:
        1 * mockConnectionComponentBuilder.suppressOperationChecks(suppressIllegalOperations) >> mockConnectionComponentBuilder

        and:
        1 * mockConnectionComponentBuilder.operationTimeout(operationTimeout) >> mockConnectionComponentBuilder

        where:
        [autoConnect, suppressIllegalOperations] << [[true, false], [true, false]].combinations()
    }

    def "should call ConnectionSubscriptionAware according to prepareConnection() subscription"() {

        given:
        clientOperationQueueMock.queue(mockConnect) >> Observable.never()

        when:
        def disposable = objectUnderTest.prepareConnection(defaultConnectionSetup).subscribe()

        then:
        1 * mockConnectionSubscriptionAware0.onConnectionSubscribed()
        1 * mockConnectionSubscriptionAware1.onConnectionSubscribed()

        when:
        disposable.dispose()

        then:
        1 * mockConnectionSubscriptionAware0.onConnectionUnsubscribed()
        1 * mockConnectionSubscriptionAware1.onConnectionUnsubscribed()
    }

    def "should call ConnectionSubscriptionAware according to prepareConnection() subscription (error case)"() {

        given:
        PublishSubject connectPublishSubject = PublishSubject.create()
        clientOperationQueueMock.queue(mockConnect) >> connectPublishSubject

        when:
        objectUnderTest.prepareConnection(defaultConnectionSetup).test()

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
        objectUnderTest.prepareConnection(defaultConnectionSetup).test()

        then:
        1 * clientOperationQueueMock.queue(mockConnect)
    }

    def "prepareConnection() should emit RxBleConnection and not complete"() {

        given:
        clientOperationQueueMock.queue(mockConnect) >> Observable.just(mockGatt)

        when:
        def testSubscriber = objectUnderTest.prepareConnection(defaultConnectionSetup).test()

        then:
        testSubscriber.assertValueCount(1)
        testSubscriber.assertNotComplete()
    }

    def "prepareConnection() should emit error from ConnectOperation queued"() {

        given:
        def testError = new Throwable("test")
        PublishSubject<BluetoothGatt> connectOperationResultSubject = PublishSubject.create()
        clientOperationQueueMock.queue(_) >> connectOperationResultSubject
        def testSubscriber = objectUnderTest.prepareConnection(defaultConnectionSetup).test()

        when:
        connectOperationResultSubject.onError(testError)

        then:
        testSubscriber.assertError(testError)
    }

    def "prepareConnection() should emit error from RxBleGattCallback.disconnectedErrorObservable()"() {

        given:
        def testError = new Throwable("test")
        clientOperationQueueMock.queue(_) >> Observable.just(mockGatt)

        when:
        def testSubscriber = objectUnderTest.prepareConnection(defaultConnectionSetup).test()

        then:
        testSubscriber.assertError(testError)
        mockCallback.observeDisconnect() >> Observable.error(testError) // Overwriting default behaviour
    }

    def "prepareConnection() should not emit exception emitted by RxBleCallback.observeDisconnect() before connecting"() {

        given:
        RuntimeException testException = new RuntimeException("test")
        clientOperationQueueMock.queue(mockConnect) >> Observable.never()
        def testSubscriber = objectUnderTest.prepareConnection(defaultConnectionSetup).test()

        when:
        disconnectErrorPublishSubject.onError(testException)

        then:
        testSubscriber.assertNoErrors()
    }

    def "prepareConnection() should emit exception emitted by RxBleCallback.observeDisconnect() after connecting"() {

        given:
        PublishSubject<BluetoothGatt> connectPublishSubject = PublishSubject.create()
        RuntimeException testException = new RuntimeException("test")
        clientOperationQueueMock.queue(mockConnect) >> connectPublishSubject
        def testSubscriber = objectUnderTest.prepareConnection(defaultConnectionSetup).test()
        connectPublishSubject.onNext(mockGatt)

        when:
        disconnectErrorPublishSubject.onError(testException)

        then:
        testSubscriber.assertError testException
    }

    def "should call ConnectionComponent.rxBleConnection() only after ConnectOperation will emit (after connecting)"() {

        given:
        PublishSubject<BluetoothGatt> connectPublishSubject = PublishSubject.create()
        clientOperationQueueMock.queue(mockConnect) >> connectPublishSubject

        when:
        objectUnderTest.prepareConnection(defaultConnectionSetup).test()

        then:
        0 * mockConnectionComponent.rxBleConnection()

        when:
        connectPublishSubject.onNext(mockGatt)

        then:
        1 * mockConnectionComponent.rxBleConnection() >> mockConnection
    }
}