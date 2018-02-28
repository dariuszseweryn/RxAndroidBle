package com.polidea.rxandroidble.internal.connection

import static com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState.STATE_OFF
import static com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_OFF
import static com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_ON

import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble.RxBleAdapterStateObservable
import com.polidea.rxandroidble.exceptions.BleDisconnectedException
import com.polidea.rxandroidble.exceptions.BleGattException
import com.polidea.rxandroidble.exceptions.BleGattOperationType
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper
import org.robospock.RoboSpecification
import rx.Observable
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject
import spock.lang.Unroll

class DisconnectionRouterTest extends RoboSpecification {

    String mockMacAddress = "1234"
    PublishSubject<RxBleAdapterStateObservable.BleAdapterState> mockAdapterStateSubject = PublishSubject.create()
    DisconnectionRouter objectUnderTest
    TestSubscriber errorTestSubscriber = new TestSubscriber()
    TestSubscriber valueTestSubscriber = new TestSubscriber()

    def createObjectUnderTest(boolean isBluetoothAdapterOnInitially) {
        def mockBleAdapterWrapper = Mock(RxBleAdapterWrapper)
        mockBleAdapterWrapper.isBluetoothEnabled() >> isBluetoothAdapterOnInitially
        objectUnderTest = new DisconnectionRouter(mockMacAddress, mockBleAdapterWrapper, mockAdapterStateSubject)
    }

    def "should emit exception from .as*Observable() when got one from .onDisconnectedException()"() {

        given:
        createObjectUnderTest(true)
        BleDisconnectedException testException = new BleDisconnectedException(mockMacAddress)
        objectUnderTest.asErrorOnlyObservable().subscribe(errorTestSubscriber)
        objectUnderTest.asValueOnlyObservable().subscribe(valueTestSubscriber)

        when:
        objectUnderTest.onDisconnectedException(testException)

        then:
        errorTestSubscriber.assertError(testException)

        and:
        valueTestSubscriber.assertValue(testException)
    }

    def "should emit exception from .asGenericObservable() when got one from .onDisconnectedException() even before subscription"() {

        given:
        createObjectUnderTest(true)
        BleDisconnectedException testException = new BleDisconnectedException(mockMacAddress)
        objectUnderTest.onDisconnectedException(testException)

        when:
        objectUnderTest.asErrorOnlyObservable().subscribe(errorTestSubscriber)

        then:
        errorTestSubscriber.assertError(testException)
    }

    def "should emit exception from .asExactObservable() when got one from .onDisconnectedException() even before subscription"() {

        given:
        createObjectUnderTest(true)
        BleDisconnectedException testException = new BleDisconnectedException(mockMacAddress)
        objectUnderTest.onDisconnectedException(testException)

        when:
        objectUnderTest.asValueOnlyObservable().subscribe(valueTestSubscriber)

        then:
        valueTestSubscriber.assertValue(testException)
    }

    def "should emit exception from .as*Observable() when got one from .onGattConnectionStatusException()"() {

        given:
        createObjectUnderTest(true)
        BleGattException testException = new BleGattException(Mock(BluetoothGatt), BluetoothGatt.GATT_FAILURE, BleGattOperationType.CONNECTION_STATE)
        objectUnderTest.asErrorOnlyObservable().subscribe(errorTestSubscriber)
        objectUnderTest.asValueOnlyObservable().subscribe(valueTestSubscriber)

        when:
        objectUnderTest.onGattConnectionStateException(testException)

        then:
        errorTestSubscriber.assertError(testException)

        and:
        valueTestSubscriber.assertValue(testException)
    }

    def "should emit exception from .asGenericObservable() when got one from .onGattConnectionStatusException() even before subscription"() {

        given:
        createObjectUnderTest(true)
        BleGattException testException = new BleGattException(Mock(BluetoothGatt), BluetoothGatt.GATT_FAILURE, BleGattOperationType.CONNECTION_STATE)
        objectUnderTest.onGattConnectionStateException(testException)

        when:
        objectUnderTest.asErrorOnlyObservable().subscribe(errorTestSubscriber)

        then:
        errorTestSubscriber.assertError(testException)
    }

    def "should emit exception from .asExactObservable() when got one from .onGattConnectionStatusException() even before subscription"() {

        given:
        createObjectUnderTest(true)
        BleGattException testException = new BleGattException(Mock(BluetoothGatt), BluetoothGatt.GATT_FAILURE, BleGattOperationType.CONNECTION_STATE)
        objectUnderTest.onGattConnectionStateException(testException)

        when:
        objectUnderTest.asValueOnlyObservable().subscribe(valueTestSubscriber)

        then:
        valueTestSubscriber.assertValue(testException)
    }

    @Unroll
    def "should emit exception from .as*Observable() when adapterStateObservable emits STATE_TURNING_ON/STATE_TURNING_OFF/STATE_OFF"() {

        given:
        createObjectUnderTest(true)
        objectUnderTest.asErrorOnlyObservable().subscribe(errorTestSubscriber)
        objectUnderTest.asValueOnlyObservable().subscribe(valueTestSubscriber)

        when:
        mockAdapterStateSubject.onNext(bleAdapterState)

        then:
        errorTestSubscriber.assertError({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress })

        and:
        valueTestSubscriber.assertAnyOnNext { BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress }

        and:
        valueTestSubscriber.assertValueCount(1)

        where:
        bleAdapterState << [ STATE_TURNING_ON, STATE_TURNING_OFF, STATE_OFF ]
    }

    @Unroll
    def "should emit exception from .asGenericObservable() when adapterStateObservable emits STATE_TURNING_ON/STATE_TURNING_OFF/STATE_OFF even before subscription"() {

        given:
        createObjectUnderTest(true)
        mockAdapterStateSubject.onNext(bleAdapterState)

        when:
        objectUnderTest.asErrorOnlyObservable().subscribe(errorTestSubscriber)

        then:
        errorTestSubscriber.assertError({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress })

        where:
        bleAdapterState << [ STATE_TURNING_ON, STATE_TURNING_OFF, STATE_OFF ]
    }

    @Unroll
    def "should emit exception from .asExactObservable() when adapterStateObservable emits STATE_TURNING_ON/STATE_TURNING_OFF/STATE_OFF even before subscription"() {

        given:
        createObjectUnderTest(true)
        mockAdapterStateSubject.onNext(bleAdapterState)

        when:
        objectUnderTest.asValueOnlyObservable().subscribe(valueTestSubscriber)

        then:
        valueTestSubscriber.assertAnyOnNext { BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress }

        and:
        valueTestSubscriber.assertValueCount(1)

        where:
        bleAdapterState << [ STATE_TURNING_ON, STATE_TURNING_OFF, STATE_OFF ]
    }

    def "should emit exception from .asGenericObservable() when RxBleAdapterWrapper.isEnabled() returns false"() {

        given:
        createObjectUnderTest(false)

        when:
        objectUnderTest.asErrorOnlyObservable().subscribe(errorTestSubscriber)

        then:
        errorTestSubscriber.assertError({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress })
    }

    def "should emit exception from .asExactObservable() when RxBleAdapterWrapper.isEnabled() returns false"() {

        given:
        createObjectUnderTest(false)

        when:
        objectUnderTest.asValueOnlyObservable().subscribe(valueTestSubscriber)

        then:
        valueTestSubscriber.assertAnyOnNext { BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress }

        and:
        valueTestSubscriber.assertValueCount(1)
    }

    def "should not emit exception from .asObservable() when adapterStateObservable emits STATE_ON"() {

        given:
        createObjectUnderTest(true)
        objectUnderTest.asErrorOnlyObservable().subscribe(errorTestSubscriber)
        objectUnderTest.asValueOnlyObservable().subscribe(valueTestSubscriber)

        when:
        mockAdapterStateSubject.onNext(RxBleAdapterStateObservable.BleAdapterState.STATE_ON)

        then:
        errorTestSubscriber.assertNoErrors()

        and:
        valueTestSubscriber.assertNoValues()
    }

    @Unroll
    def "should unsubscribe from adapterStateObservable if it emits STATE_OFF/STATE_TURNING_* or if .on*Exception() is called"() {

        given:
        createObjectUnderTest(true)

        when:
        disconnectionScenario.call(mockAdapterStateSubject, objectUnderTest)

        then:
        !mockAdapterStateSubject.hasObservers()

        where:
        disconnectionScenario << [
                { PublishSubject<RxBleAdapterStateObservable.BleAdapterState> mockAdapterStateSubject, DisconnectionRouter objectUnderTest ->
                    mockAdapterStateSubject.onNext(STATE_TURNING_ON)
                },
                { PublishSubject<RxBleAdapterStateObservable.BleAdapterState> mockAdapterStateSubject, DisconnectionRouter objectUnderTest ->
                    mockAdapterStateSubject.onNext(STATE_TURNING_OFF)
                },
                { PublishSubject<RxBleAdapterStateObservable.BleAdapterState> mockAdapterStateSubject, DisconnectionRouter objectUnderTest ->
                    mockAdapterStateSubject.onNext(STATE_OFF)
                },
                { PublishSubject<RxBleAdapterStateObservable.BleAdapterState> mockAdapterStateSubject, DisconnectionRouter objectUnderTest ->
                    objectUnderTest.onGattConnectionStateException(new BleGattException(null, 0, BleGattOperationType.CHARACTERISTIC_WRITE))
                },
                { PublishSubject<RxBleAdapterStateObservable.BleAdapterState> mockAdapterStateSubject, DisconnectionRouter objectUnderTest ->
                    objectUnderTest.onDisconnectedException(new BleDisconnectedException("test"))
                },
        ]
    }
}
