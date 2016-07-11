package com.polidea.rxandroidble.internal.connection
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import com.polidea.rxandroidble.exceptions.BleDisconnectedException
import com.polidea.rxandroidble.internal.RxBleRadio
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect
import com.polidea.rxandroidble.internal.util.BleConnectionCompat
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper
import rx.Observable
import rx.observers.TestSubscriber
import spock.lang.Specification
import spock.lang.Unroll

import static com.polidea.rxandroidble.internal.connection.RxBleConnectionConnectorOperationsProvider.*

public class RxBleConnectionConnectorImplTest extends Specification {

    RxBleRadio mockRadio
    Context mockContext
    BluetoothDevice mockDevice
    RxBleGattCallback mockCallback
    RxBleGattCallback.Provider mockCallbackProvider
    RxBleRadioOperationConnect mockConnect
    RxBleRadioOperationDisconnect mockDisconnect
    RxBleConnectionConnectorOperationsProvider mockOperationsProvider
    RxBleConnectionConnectorImpl objectUnderTest
    BleConnectionCompat mockConnectionCompat
    RxBleAdapterWrapper mockAdapterWrapper

    def setup() {
        mockConnectionCompat = Mock BleConnectionCompat
        mockRadio = Mock RxBleRadio
        mockContext = Mock Context
        mockDevice = Mock BluetoothDevice

        mockCallback = Mock RxBleGattCallback
        mockCallbackProvider = Mock RxBleGattCallback.Provider
        mockCallbackProvider.provide() >> mockCallback

        mockConnect = Mock RxBleRadioOperationConnect
        mockDisconnect = Mock RxBleRadioOperationDisconnect
        mockOperationsProvider = Mock RxBleConnectionConnectorOperationsProvider
        mockOperationsProvider.provide(*_) >> new RxBleOperations(mockConnect, mockDisconnect)

        mockAdapterWrapper = Mock RxBleAdapterWrapper

        objectUnderTest = new RxBleConnectionConnectorImpl(mockDevice, mockCallbackProvider, mockOperationsProvider, mockRadio, mockConnectionCompat, mockAdapterWrapper)
    }

    @Unroll
    def "prepareConnection() should pass arguments to RxBleConnectionConnectorOperationsProvider #id"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        def testSubscriber = new TestSubscriber()

        when:
        objectUnderTest.prepareConnection(contextObject, autoConnectValue).subscribe(testSubscriber)

        then:
        1 * mockOperationsProvider.provide(contextObject, mockDevice, autoConnectValue, mockConnectionCompat, mockCallback)

        where:
        contextObject | autoConnectValue
        null          | true
        null          | false
        Mock(Context) | true
        Mock(Context) | false
    }

    def "subscribing prepareConnection() should schedule provided RxBleRadioOperationConnect on RxBleRadio"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        def testSubscriber = new TestSubscriber()

        when:
        objectUnderTest.prepareConnection(null, true).subscribe(testSubscriber)

        then:
        1 * mockRadio.queue(mockConnect)
    }

    def "prepareConnection() should schedule provided RxBleRadioOperationDisconnect on RxBleRadio if RxBleRadio.queue(RxBleRadioOperation) emits error"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        def testSubscriber = new TestSubscriber()
        mockRadio.queue(mockConnect) >> Observable.error(new Throwable("test"))

        when:
        objectUnderTest.prepareConnection(null, true).subscribe(testSubscriber)

        then:
        1 * mockRadio.queue(mockDisconnect) >> Observable.just(null)
    }

    def "prepareConnection() should schedule provided RxBleRadioOperationDisconnect on RxBleRadio only once if RxBleRadio.queue(RxBleRadioOperation) emits error and subscriber will unsubscribe"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        def testSubscriber = new TestSubscriber()
        mockRadio.queue(mockConnect) >> Observable.error(new Throwable("test"))

        when:
        objectUnderTest.prepareConnection(null, true).subscribe(testSubscriber)

        then:
        1 * mockRadio.queue(mockDisconnect) >> Observable.just(null)
    }

    def "prepareConnection() should schedule provided RxBleRadioOperationDisconnect on RxBleRadio when subscriber will unsubscribe"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        def testSubscriber = new TestSubscriber()
        mockRadio.queue(mockConnect) >> Observable.empty()

        when:
        objectUnderTest.prepareConnection(null, true).subscribe(testSubscriber)
        testSubscriber.unsubscribe()

        then:
        1 * mockRadio.queue(mockDisconnect) >> Observable.just(null)
    }

    def "prepareConnection() should emit RxBleConnection and not complete"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        def testSubscriber = new TestSubscriber()
        def mockGatt = Mock(BluetoothGatt)
        mockRadio.queue(mockConnect) >> Observable.just(mockGatt)

        when:
        objectUnderTest.prepareConnection(null, true).subscribe(testSubscriber)

        then:
        testSubscriber.assertValueCount(1)
        testSubscriber.assertNotCompleted()
    }

    def "prepareConnection() should emit error from RxBleGattCallback.disconnectedErrorObservable()"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> true
        def testSubscriber = new TestSubscriber()
        def mockGatt = Mock(BluetoothGatt)
        def testError = new Throwable("test")
        mockRadio.queue(_) >> Observable.just(mockGatt)
        mockCallback.observeDisconnect() >> Observable.error(testError)

        when:
        objectUnderTest.prepareConnection(null, true).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(testError)
    }


    def "prepareConnection() should emit error from BleDisconnectedException when RxBleAdapterWrapper.isEnabled() returns false"() {

        given:
        mockAdapterWrapper.isBluetoothEnabled() >> false
        def testSubscriber = new TestSubscriber()

        when:
        objectUnderTest.prepareConnection(null, autoConnect).subscribe(testSubscriber)

        then:
        testSubscriber.assertError(BleDisconnectedException)

        where:
        autoConnect << [
                true,
                false
        ]
    }
}