package com.polidea.rxandroidble3.internal.connection

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import com.polidea.rxandroidble3.RxBleAdapterStateObservable
import com.polidea.rxandroidble3.exceptions.BleDisconnectedException
import com.polidea.rxandroidble3.exceptions.BleGattException
import com.polidea.rxandroidble3.exceptions.BleGattOperationType
import com.polidea.rxandroidble3.internal.util.RxBleAdapterWrapper
import hkhc.electricspock.ElectricSpecification
import io.reactivex.rxjava3.subjects.PublishSubject
import org.robolectric.annotation.Config
import spock.lang.Shared
import spock.lang.Unroll

import static com.polidea.rxandroidble3.RxBleAdapterStateObservable.BleAdapterState.*

@Config(manifest = Config.NONE)
class DisconnectionRouterTest extends ElectricSpecification {

    String mockMacAddress = "1234"

    PublishSubject<RxBleAdapterStateObservable.BleAdapterState> mockAdapterStateSubject = PublishSubject.create()

    DisconnectionRouter objectUnderTest

    @Shared
    BluetoothGatt mockBluetoothGatt = Mock BluetoothGatt

    @Shared
    BluetoothDevice mockBluetoothDevice = Mock BluetoothDevice

    @Shared
    String mockAddress = "deviceAddress"

    def setupSpec() {
        mockBluetoothGatt.getDevice() >> mockBluetoothDevice
        mockBluetoothDevice.getAddress() >> mockAddress
    }

    def createObjectUnderTest(boolean isBluetoothAdapterOnInitially) {
        def mockBleAdapterWrapper = Mock(RxBleAdapterWrapper)
        mockBleAdapterWrapper.isBluetoothEnabled() >> isBluetoothAdapterOnInitially
        objectUnderTest = new DisconnectionRouter(mockMacAddress, mockBleAdapterWrapper, mockAdapterStateSubject)
    }

    def "should emit exception from .as*Observable() when got one from .onDisconnectedException()"() {

        given:
        createObjectUnderTest(true)
        BleDisconnectedException testException = new BleDisconnectedException(mockMacAddress)
        def errorTestSubscriber = objectUnderTest.asErrorOnlyObservable().test()
        def valueTestSubscriber = objectUnderTest.asValueOnlyObservable().test()

        when:
        objectUnderTest.onDisconnectedException(testException)

        then:
        errorTestSubscriber.assertError(testException)

        and:
        valueTestSubscriber.assertValue(testException)
    }

    def "should emit exception from .asErrorOnlyObservable() when got one from .onDisconnectedException() even before subscription"() {

        given:
        createObjectUnderTest(true)
        BleDisconnectedException testException = new BleDisconnectedException(mockMacAddress)
        objectUnderTest.onDisconnectedException(testException)

        when:
        def errorTestSubscriber = objectUnderTest.asErrorOnlyObservable().test()

        then:
        errorTestSubscriber.assertError(testException)
    }

    def "should emit exception from .asValueOnlyObservable() when got one from .onDisconnectedException() even before subscription"() {

        given:
        createObjectUnderTest(true)
        BleDisconnectedException testException = new BleDisconnectedException(mockMacAddress)
        objectUnderTest.onDisconnectedException(testException)

        when:
        def valueTestSubscriber = objectUnderTest.asValueOnlyObservable().test()

        then:
        valueTestSubscriber.assertValue(testException)
    }

    def "should emit exception from .as*Observable() when got one from .onGattConnectionStatusException()"() {

        given:
        createObjectUnderTest(true)
        BleGattException testException = new BleGattException(mockBluetoothGatt, BluetoothGatt.GATT_FAILURE, BleGattOperationType.CONNECTION_STATE)
        def errorTestSubscriber = objectUnderTest.asErrorOnlyObservable().test()
        def valueTestSubscriber = objectUnderTest.asValueOnlyObservable().test()

        when:
        objectUnderTest.onGattConnectionStateException(testException)

        then:
        errorTestSubscriber.assertError(testException)

        and:
        valueTestSubscriber.assertValue(testException)
    }

    def "should emit exception from .asErrorOnlyObservable() when got one from .onGattConnectionStatusException() even before subscription"() {

        given:
        createObjectUnderTest(true)
        BleGattException testException = new BleGattException(mockBluetoothGatt, BluetoothGatt.GATT_FAILURE, BleGattOperationType.CONNECTION_STATE)
        objectUnderTest.onGattConnectionStateException(testException)

        when:
        def errorTestSubscriber = objectUnderTest.asErrorOnlyObservable().test()

        then:
        errorTestSubscriber.assertError(testException)
    }

    def "should emit exception from .asValueOnlyObservable() when got one from .onGattConnectionStatusException() even before subscription"() {

        given:
        createObjectUnderTest(true)
        BleGattException testException = new BleGattException(mockBluetoothGatt, BluetoothGatt.GATT_FAILURE, BleGattOperationType.CONNECTION_STATE)
        objectUnderTest.onGattConnectionStateException(testException)

        when:
        def valueTestSubscriber = objectUnderTest.asValueOnlyObservable().test()

        then:
        valueTestSubscriber.assertValue(testException)
    }

    @Unroll
    def "should emit exception from .as*Observable() when adapterStateObservable emits STATE_TURNING_ON/STATE_TURNING_OFF/STATE_OFF"() {

        given:
        createObjectUnderTest(true)
        def errorTestSubscriber = objectUnderTest.asErrorOnlyObservable().test()
        def valueTestSubscriber = objectUnderTest.asValueOnlyObservable().test()

        when:
        mockAdapterStateSubject.onNext(bleAdapterState)

        then:
        errorTestSubscriber.assertError({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress })

        and:
        valueTestSubscriber.assertAnyOnNext { BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress }

        and:
        valueTestSubscriber.assertValueCount(1)

        where:
        bleAdapterState << [STATE_TURNING_ON, STATE_TURNING_OFF, STATE_OFF]
    }

    @Unroll
    def "should emit exception from .asErrorOnlyObservable() when adapterStateObservable emits STATE_TURNING_ON/STATE_TURNING_OFF/STATE_OFF even before subscription"() {

        given:
        createObjectUnderTest(true)
        mockAdapterStateSubject.onNext(bleAdapterState)

        when:
        def errorTestSubscriber = objectUnderTest.asErrorOnlyObservable().test()

        then:
        errorTestSubscriber.assertError({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress })

        where:
        bleAdapterState << [STATE_TURNING_ON, STATE_TURNING_OFF, STATE_OFF]
    }

    @Unroll
    def "should emit exception from .asValueOnlyObservable() when adapterStateObservable emits STATE_TURNING_ON/STATE_TURNING_OFF/STATE_OFF even before subscription"() {

        given:
        createObjectUnderTest(true)
        mockAdapterStateSubject.onNext(bleAdapterState)

        when:
        def valueTestSubscriber = objectUnderTest.asValueOnlyObservable().test()

        then:
        valueTestSubscriber.assertAnyOnNext { BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress }

        and:
        valueTestSubscriber.assertValueCount(1)

        where:
        bleAdapterState << [STATE_TURNING_ON, STATE_TURNING_OFF, STATE_OFF]
    }

    def "should emit exception from .asErrorOnlyObservable() when RxBleAdapterWrapper.isEnabled() returns false"() {

        given:
        createObjectUnderTest(false)

        when:
        def errorTestSubscriber = objectUnderTest.asErrorOnlyObservable().test()

        then:
        errorTestSubscriber.assertError({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress })
    }

    def "should emit exception from .asValueOnlyObservable() when RxBleAdapterWrapper.isEnabled() returns false"() {

        given:
        createObjectUnderTest(false)

        when:
        def valueTestSubscriber = objectUnderTest.asValueOnlyObservable().test()

        then:
        valueTestSubscriber.assertAnyOnNext({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress })

        and:
        valueTestSubscriber.assertValueCount(1)
    }

    def "should not emit exception from .asObservable() when adapterStateObservable emits STATE_ON"() {

        given:
        createObjectUnderTest(true)
        def errorTestSubscriber = objectUnderTest.asErrorOnlyObservable().test()
        def valueTestSubscriber = objectUnderTest.asValueOnlyObservable().test()

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
                    objectUnderTest.onGattConnectionStateException(new BleGattException(mockBluetoothGatt, 0, BleGattOperationType.CHARACTERISTIC_WRITE))
                },
                { PublishSubject<RxBleAdapterStateObservable.BleAdapterState> mockAdapterStateSubject, DisconnectionRouter objectUnderTest ->
                    objectUnderTest.onDisconnectedException(new BleDisconnectedException("test"))
                },
        ]
    }
}
