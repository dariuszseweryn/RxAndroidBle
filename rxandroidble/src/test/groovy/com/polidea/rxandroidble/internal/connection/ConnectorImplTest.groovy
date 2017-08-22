package com.polidea.rxandroidble.internal.connection

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble.internal.ConnectionSetup
import com.polidea.rxandroidble.RxBleAdapterStateObservable
import com.polidea.rxandroidble.RxBleConnection
import com.polidea.rxandroidble.exceptions.BleDisconnectedException
import com.polidea.rxandroidble.internal.serialization.ClientOperationQueue
import com.polidea.rxandroidble.internal.operations.MockConnectionComponentBuilder
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper
import rx.Observable
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject
import spock.lang.Specification
import spock.lang.Unroll

public class ConnectorImplTest extends Specification {

    ClientOperationQueue clientOperationQueueMock = Mock ClientOperationQueue
    BluetoothDevice mockDevice = Mock BluetoothDevice
    RxBleGattCallback mockCallback = Mock RxBleGattCallback
    RxBleRadioOperationConnect mockConnect = Mock RxBleRadioOperationConnect
    RxBleRadioOperationDisconnect mockDisconnect = Mock RxBleRadioOperationDisconnect
    RxBleAdapterWrapper mockAdapterWrapper = Mock RxBleAdapterWrapper
    PublishSubject<RxBleAdapterStateObservable.BleAdapterState> adapterStatePublishSubject = PublishSubject.create()
    TestSubscriber<RxBleConnection> testSubscriber = TestSubscriber.create()
    BluetoothGatt mockGatt = Mock BluetoothGatt
    ConnectionComponent.Builder mockConnectionComponentBuilder
    ConnectionSetup defaultConnectionSetup = new ConnectionSetup.Builder().build()

    ConnectorImpl objectUnderTest

    def setup() {
        clientOperationQueueMock.queue(mockDisconnect) >> Observable.just(mockGatt)
        mockCallback.observeDisconnect() >> Observable.never()
        mockConnectionComponentBuilder = new MockConnectionComponentBuilder(
                clientOperationQueueMock,
                Mock(RxBleConnection),
                mockDevice,
                mockCallback,
                mockDisconnect,
                mockConnect
        )

        objectUnderTest = new ConnectorImpl(
                mockDevice,
                clientOperationQueueMock,
                mockAdapterWrapper,
                adapterStatePublishSubject,
                mockConnectionComponentBuilder
        )

    }

    def "subscribing prepareConnection() should schedule provided RxBleRadioOperationConnect on ClientOperationQueue"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true

        when:
        objectUnderTest.prepareConnection(defaultConnectionSetup).subscribe(testSubscriber)

        then:
        1 * clientOperationQueueMock.queue(mockConnect)
    }

    def "prepareConnection() should schedule provided RxBleRadioOperationDisconnect on ClientOperationQueue if ClientOperationQueue.queue(RxBleRadioOperation) emits error"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        clientOperationQueueMock.queue(mockConnect) >> Observable.error(new Throwable("test"))

        when:
        objectUnderTest.prepareConnection(defaultConnectionSetup).subscribe(testSubscriber)

        then:
        1 * clientOperationQueueMock.queue(mockDisconnect) >> Observable.just(null)
    }

    def "prepareConnection() should schedule provided RxBleRadioOperationDisconnect on ClientOperationQueue only once if ClientOperationQueue.queue(RxBleRadioOperation) emits error and subscriber will unsubscribe"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        clientOperationQueueMock.queue(mockConnect) >> Observable.error(new Throwable("test"))

        when:
        objectUnderTest.prepareConnection(defaultConnectionSetup).subscribe(testSubscriber)

        then:
        1 * clientOperationQueueMock.queue(mockDisconnect) >> Observable.just(null)
    }

    def "prepareConnection() should schedule provided RxBleRadioOperationDisconnect on operation queue when subscriber will unsubscribe"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        clientOperationQueueMock.queue(mockConnect) >> Observable.empty()

        when:
        objectUnderTest.prepareConnection(defaultConnectionSetup).subscribe(testSubscriber)
        testSubscriber.unsubscribe()

        then:
        1 * clientOperationQueueMock.queue(mockDisconnect) >> Observable.just(null)
    }

    def "prepareConnection() should emit RxBleConnection and not complete"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        clientOperationQueueMock.queue(mockConnect) >> Observable.just(mockGatt)

        when:
        objectUnderTest.prepareConnection(defaultConnectionSetup).subscribe(testSubscriber)

        then:
        testSubscriber.assertValueCount(1)
        testSubscriber.assertNotCompleted()
    }

    def "prepareConnection() should emit error from RxBleGattCallback.disconnectedErrorObservable()"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        def testError = new Throwable("test")
        clientOperationQueueMock.queue(_) >> Observable.just(mockGatt)

        when:
        objectUnderTest.prepareConnection(defaultConnectionSetup).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(testError)
        mockCallback.observeDisconnect() >> Observable.error(testError) // Overwriting default behaviour
    }


    def "prepareConnection() should emit error from BleDisconnectedException when RxBleAdapterWrapper.isEnabled() returns false"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> false
        def connectionSetup = new ConnectionSetup.Builder().setAutoConnect(autoConnect).build()

        when:
        objectUnderTest.prepareConnection(connectionSetup).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleDisconnectedException)

        where:
        autoConnect << [
                true,
                false
        ]
    }

    @Unroll
    def "prepareConnection() should emit BleDisconnectedException when RxBleAdapterStateObservable emits not usable RxBleAdapterStateObservable.BleAdapterState"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        clientOperationQueueMock.queue(_) >> Observable.never()
        def connectionSetup = new ConnectionSetup.Builder().setAutoConnect(autoConnect).build()
        objectUnderTest.prepareConnection(connectionSetup).subscribe(testSubscriber)

        when:
        adapterStatePublishSubject.onNext(state)

        then:
        testSubscriber.assertError BleDisconnectedException

        where:
        autoConnect | state
        true        | RxBleAdapterStateObservable.BleAdapterState.STATE_OFF
        true        | RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_OFF
        true        | RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_ON
        false       | RxBleAdapterStateObservable.BleAdapterState.STATE_OFF
        false       | RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_OFF
        false       | RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_ON
    }

    @Unroll
    def "prepareConnection() should not emit BleDisconnectedException when RxBleAdapterStateObservable emits usable RxBleAdapterStateObservable.BleAdapterState"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        clientOperationQueueMock.queue(_) >> Observable.never()
        def connectionSetup = new ConnectionSetup.Builder().setAutoConnect(autoConnect).build()
        objectUnderTest.prepareConnection(connectionSetup).subscribe(testSubscriber)

        when:
        adapterStatePublishSubject.onNext(RxBleAdapterStateObservable.BleAdapterState.STATE_ON)

        then:
        testSubscriber.assertNoErrors()

        where:
        autoConnect << [
                true,
                false
        ]
    }
}