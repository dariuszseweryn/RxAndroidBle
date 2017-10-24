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
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject
import spock.lang.Unroll

class DisconnectionRouterTest extends RoboSpecification {

    String mockMacAddress = "1234"
    PublishSubject<RxBleAdapterStateObservable.BleAdapterState> mockAdapterStateSubject = PublishSubject.create()
    DisconnectionRouter objectUnderTest
    TestSubscriber genericTestSubscriber = new TestSubscriber()
    TestSubscriber exactTestSubscriber = new TestSubscriber()

    def createObjectUnderTest(boolean isBluetoothAdapterOnInitially) {
        def mockBleAdapterWrapper = Mock(RxBleAdapterWrapper)
        mockBleAdapterWrapper.isBluetoothEnabled() >> isBluetoothAdapterOnInitially
        objectUnderTest = new DisconnectionRouter(mockMacAddress, mockBleAdapterWrapper, mockAdapterStateSubject)
    }

    def "should emit exception from .as*Observable() when got one from .onDisconnectedException()"() {

        given:
        createObjectUnderTest(true)
        BleDisconnectedException testException = new BleDisconnectedException(mockMacAddress)
        objectUnderTest.asGenericObservable().subscribe(genericTestSubscriber)
        objectUnderTest.asExactObservable().subscribe(exactTestSubscriber)

        when:
        objectUnderTest.onDisconnectedException(testException)

        then:
        genericTestSubscriber.assertError(testException)

        and:
        exactTestSubscriber.assertValue(testException)
    }

    def "should emit exception from .asGenericObservable() when got one from .onDisconnectedException() even before subscription"() {

        given:
        createObjectUnderTest(true)
        BleDisconnectedException testException = new BleDisconnectedException(mockMacAddress)
        objectUnderTest.onDisconnectedException(testException)

        when:
        objectUnderTest.asGenericObservable().subscribe(genericTestSubscriber)

        then:
        genericTestSubscriber.assertError(testException)
    }

    def "should emit exception from .asExactObservable() when got one from .onDisconnectedException() even before subscription"() {

        given:
        createObjectUnderTest(true)
        BleDisconnectedException testException = new BleDisconnectedException(mockMacAddress)
        objectUnderTest.onDisconnectedException(testException)

        when:
        objectUnderTest.asExactObservable().subscribe(exactTestSubscriber)

        then:
        exactTestSubscriber.assertValue(testException)
    }

    def "should emit exception from .as*Observable() when got one from .onGattConnectionStatusException()"() {

        given:
        createObjectUnderTest(true)
        BleGattException testException = new BleGattException(Mock(BluetoothGatt), BluetoothGatt.GATT_FAILURE, BleGattOperationType.CONNECTION_STATE)
        objectUnderTest.asGenericObservable().subscribe(genericTestSubscriber)
        objectUnderTest.asExactObservable().subscribe(exactTestSubscriber)

        when:
        objectUnderTest.onGattConnectionStateException(testException)

        then:
        genericTestSubscriber.assertError(testException)

        and:
        exactTestSubscriber.assertValue(testException)
    }

    def "should emit exception from .asGenericObservable() when got one from .onGattConnectionStatusException() even before subscription"() {

        given:
        createObjectUnderTest(true)
        BleGattException testException = new BleGattException(Mock(BluetoothGatt), BluetoothGatt.GATT_FAILURE, BleGattOperationType.CONNECTION_STATE)
        objectUnderTest.onGattConnectionStateException(testException)

        when:
        objectUnderTest.asGenericObservable().subscribe(genericTestSubscriber)

        then:
        genericTestSubscriber.assertError(testException)
    }

    def "should emit exception from .asExactObservable() when got one from .onGattConnectionStatusException() even before subscription"() {

        given:
        createObjectUnderTest(true)
        BleGattException testException = new BleGattException(Mock(BluetoothGatt), BluetoothGatt.GATT_FAILURE, BleGattOperationType.CONNECTION_STATE)
        objectUnderTest.onGattConnectionStateException(testException)

        when:
        objectUnderTest.asExactObservable().subscribe(exactTestSubscriber)

        then:
        exactTestSubscriber.assertValue(testException)
    }

    @Unroll
    def "should emit exception from .as*Observable() when adapterStateObservable emits STATE_TURNING_ON/STATE_TURNING_OFF/STATE_OFF"() {

        given:
        createObjectUnderTest(true)
        objectUnderTest.asGenericObservable().subscribe(genericTestSubscriber)
        objectUnderTest.asExactObservable().subscribe(exactTestSubscriber)

        when:
        mockAdapterStateSubject.onNext(bleAdapterState)

        then:
        genericTestSubscriber.assertError({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress })

        and:
        exactTestSubscriber.assertAnyOnNext { BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress }

        and:
        exactTestSubscriber.assertValueCount(1)

        where:
        bleAdapterState << [ STATE_TURNING_ON, STATE_TURNING_OFF, STATE_OFF ]
    }

    @Unroll
    def "should emit exception from .asGenericObservable() when adapterStateObservable emits STATE_TURNING_ON/STATE_TURNING_OFF/STATE_OFF even before subscription"() {

        given:
        createObjectUnderTest(true)
        mockAdapterStateSubject.onNext(bleAdapterState)

        when:
        objectUnderTest.asGenericObservable().subscribe(genericTestSubscriber)

        then:
        genericTestSubscriber.assertError({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress })

        where:
        bleAdapterState << [ STATE_TURNING_ON, STATE_TURNING_OFF, STATE_OFF ]
    }

    @Unroll
    def "should emit exception from .asExactObservable() when adapterStateObservable emits STATE_TURNING_ON/STATE_TURNING_OFF/STATE_OFF even before subscription"() {

        given:
        createObjectUnderTest(true)
        mockAdapterStateSubject.onNext(bleAdapterState)

        when:
        objectUnderTest.asExactObservable().subscribe(exactTestSubscriber)

        then:
        exactTestSubscriber.assertAnyOnNext { BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress }

        and:
        exactTestSubscriber.assertValueCount(1)

        where:
        bleAdapterState << [ STATE_TURNING_ON, STATE_TURNING_OFF, STATE_OFF ]
    }

    def "should emit exception from .asGenericObservable() when RxBleAdapterWrapper.isEnabled() returns false"() {

        given:
        createObjectUnderTest(false)

        when:
        objectUnderTest.asGenericObservable().subscribe(genericTestSubscriber)

        then:
        genericTestSubscriber.assertError({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress })
    }

    def "should emit exception from .asExactObservable() when RxBleAdapterWrapper.isEnabled() returns false"() {

        given:
        createObjectUnderTest(false)

        when:
        objectUnderTest.asExactObservable().subscribe(exactTestSubscriber)

        then:
        exactTestSubscriber.assertAnyOnNext { BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress }

        and:
        exactTestSubscriber.assertValueCount(1)
    }

    def "should not emit exception from .asObservable() when adapterStateObservable emits STATE_ON"() {

        given:
        createObjectUnderTest(true)
        objectUnderTest.asGenericObservable().subscribe(genericTestSubscriber)
        objectUnderTest.asExactObservable().subscribe(exactTestSubscriber)

        when:
        mockAdapterStateSubject.onNext(RxBleAdapterStateObservable.BleAdapterState.STATE_ON)

        then:
        genericTestSubscriber.assertNoErrors()

        and:
        exactTestSubscriber.assertNoValues()
    }
}
