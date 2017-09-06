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
    TestSubscriber testSubscriber = new TestSubscriber()

    def createObjectUnderTest(boolean isBluetoothAdapterOnInitially) {
        def mockBleAdapterWrapper = Mock(RxBleAdapterWrapper)
        mockBleAdapterWrapper.isBluetoothEnabled() >> isBluetoothAdapterOnInitially
        objectUnderTest = new DisconnectionRouter(mockMacAddress, mockBleAdapterWrapper, mockAdapterStateSubject)
    }

    def "should emit exception from .asObservable() when got one from .onDisconnectedException()"() {

        given:
        createObjectUnderTest(true)
        BleDisconnectedException testException = new BleDisconnectedException(mockMacAddress)
        objectUnderTest.asObservable().subscribe(testSubscriber)

        when:
        objectUnderTest.onDisconnectedException(testException)

        then:
        testSubscriber.assertError(testException)
    }

    def "should emit exception from .asObservable() when got one from .onDisconnectedException() even before subscription"() {

        given:
        createObjectUnderTest(true)
        BleDisconnectedException testException = new BleDisconnectedException(mockMacAddress)
        objectUnderTest.onDisconnectedException(testException)

        when:
        objectUnderTest.asObservable().subscribe(testSubscriber)

        then:
        testSubscriber.assertError(testException)
    }

    def "should emit exception from .asObservable() when got one from .onGattConnectionStatusException()"() {

        given:
        createObjectUnderTest(true)
        BleGattException testException = new BleGattException(Mock(BluetoothGatt), BluetoothGatt.GATT_FAILURE, BleGattOperationType.CONNECTION_STATE)
        objectUnderTest.asObservable().subscribe(testSubscriber)

        when:
        objectUnderTest.onGattConnectionStateException(testException)

        then:
        testSubscriber.assertError(testException)
    }

    def "should emit exception from .asObservable() when got one from .onGattConnectionStatusException() even before subscription"() {

        given:
        createObjectUnderTest(true)
        BleGattException testException = new BleGattException(Mock(BluetoothGatt), BluetoothGatt.GATT_FAILURE, BleGattOperationType.CONNECTION_STATE)
        objectUnderTest.onGattConnectionStateException(testException)

        when:
        objectUnderTest.asObservable().subscribe(testSubscriber)

        then:
        testSubscriber.assertError(testException)
    }

    @Unroll
    def "should emit exception from .asObservable() when adapterStateObservable emits STATE_TURNING_ON/STATE_TURNING_OFF/STATE_OFF"() {

        given:
        createObjectUnderTest(true)
        objectUnderTest.asObservable().subscribe(testSubscriber)

        when:
        mockAdapterStateSubject.onNext(bleAdapterState)

        then:
        testSubscriber.assertError({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress })

        where:
        bleAdapterState << [ STATE_TURNING_ON, STATE_TURNING_OFF, STATE_OFF ]
    }

    @Unroll
    def "should emit exception from .asObservable() when adapterStateObservable emits STATE_TURNING_ON/STATE_TURNING_OFF/STATE_OFF even before subscription"() {

        given:
        createObjectUnderTest(true)
        mockAdapterStateSubject.onNext(bleAdapterState)

        when:
        objectUnderTest.asObservable().subscribe(testSubscriber)

        then:
        testSubscriber.assertError({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress })

        where:
        bleAdapterState << [ STATE_TURNING_ON, STATE_TURNING_OFF, STATE_OFF ]
    }

    def "should emit exception from .asObservable() when RxBleAdapterWrapper.isEnabled() returns false"() {

        given:
        createObjectUnderTest(false)

        when:
        objectUnderTest.asObservable().subscribe(testSubscriber)

        then:
        testSubscriber.assertError({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress })
    }

    def "should not emit exception from .asObservable() when adapterStateObservable emits STATE_ON"() {

        given:
        createObjectUnderTest(true)
        objectUnderTest.asObservable().subscribe(testSubscriber)

        when:
        mockAdapterStateSubject.onNext(RxBleAdapterStateObservable.BleAdapterState.STATE_ON)

        then:
        testSubscriber.assertNoErrors()
    }
}
