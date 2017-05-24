package com.polidea.rxandroidble.internal.util

import static com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState.STATE_OFF
import static com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState.STATE_ON
import static com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_OFF
import static com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_ON

import com.polidea.rxandroidble.RxBleAdapterStateObservable
import com.polidea.rxandroidble.RxBleClient
import java.util.concurrent.TimeUnit
import rx.observers.TestSubscriber
import rx.schedulers.TestScheduler
import rx.subjects.BehaviorSubject
import spock.lang.Specification
import spock.lang.Unroll

class ClientStateObservableTest extends Specification {

    def adapterWrapperMock = Mock RxBleAdapterWrapper

    BehaviorSubject<RxBleAdapterStateObservable.BleAdapterState> adapterStateSubject = BehaviorSubject.create()

    BehaviorSubject<Boolean> locationServicesOkSubject = BehaviorSubject.create()

    def locationServicesStatusMock = Mock LocationServicesStatus

    def testScheduler = new TestScheduler()

    def testSubscriber = new TestSubscriber()

    def objectUnderTest = new ClientStateObservable(adapterWrapperMock, adapterStateSubject, locationServicesOkSubject, locationServicesStatusMock, testScheduler)

    @Unroll
    def "should complete if BluetoothAdapter is not available"() {

        given:
        adapterWrapperMock.hasBluetoothAdapter() >> false
        adapterWrapperMock.isBluetoothEnabled() >> (initialAdapterState == STATE_ON)
        adapterStateSubject.onNext(initialAdapterState)
        locationServicesOkSubject.onNext(initialLocationServicesOkState)
        locationServicesStatusMock.isLocationPermissionOk() >> locationPermissionState

        when:
        objectUnderTest.subscribe(testSubscriber)

        then:
        testSubscriber.assertCompleted()

        where:
        [initialAdapterState, initialLocationServicesOkState, locationPermissionState] << [
                [
                        STATE_OFF,
                        STATE_ON,
                        STATE_TURNING_OFF,
                        STATE_TURNING_ON
                ],
                [
                        true, false
                ],
                [
                        true, false
                ]
        ].combinations()
    }

    @Unroll
    def "should not complete if BluetoothAdapter is available"() {

        given:
        adapterWrapperMock.hasBluetoothAdapter() >> true
        adapterWrapperMock.isBluetoothEnabled() >> (adapterState == STATE_ON)
        locationServicesOkSubject.onNext(servicesState)
        locationServicesStatusMock.isLocationPermissionOk() >> locationPermissionState

        when:
        objectUnderTest.subscribe(testSubscriber)

        then:
        testSubscriber.assertNotCompleted()

        where:
        [adapterState, servicesState, locationPermissionState] << [
                [
                        STATE_OFF,
                        STATE_ON,
                        STATE_TURNING_OFF,
                        STATE_TURNING_ON
                ],
                [
                        true, false
                ],
                [
                        true, false
                ]
        ].combinations()
    }

    @Unroll
    def "should emit proper state when the permission is granted at some point"() {

        given:
        adapterWrapperMock.hasBluetoothAdapter() >> true
        adapterWrapperMock.isBluetoothEnabled() >> (adapterState == STATE_ON)
        locationServicesStatusMock.isLocationPermissionOk() >> false
        locationServicesOkSubject.onNext(servicesState)
        objectUnderTest.subscribe(testSubscriber)

        when:
        testScheduler.triggerActions()

        then:
        testSubscriber.assertNoValues()

        when:
        testScheduler.advanceTimeBy(1L, TimeUnit.SECONDS)

        then:
        1 * locationServicesStatusMock.isLocationPermissionOk() >> true

        and:
        testSubscriber.assertValue(expectedValue)

        where:
        adapterState      | servicesState | expectedValue
        STATE_OFF         | true          | RxBleClient.State.BLUETOOTH_NOT_ENABLED
        STATE_OFF         | false         | RxBleClient.State.BLUETOOTH_NOT_ENABLED
        STATE_TURNING_OFF | true          | RxBleClient.State.BLUETOOTH_NOT_ENABLED
        STATE_TURNING_OFF | false         | RxBleClient.State.BLUETOOTH_NOT_ENABLED
        STATE_TURNING_ON  | true          | RxBleClient.State.BLUETOOTH_NOT_ENABLED
        STATE_TURNING_ON  | false         | RxBleClient.State.BLUETOOTH_NOT_ENABLED
        STATE_ON          | true          | RxBleClient.State.READY
        STATE_ON          | false         | RxBleClient.State.LOCATION_SERVICES_NOT_ENABLED
    }

    def "should emit LOCATION_SERVICES_NOT_ENABLED state when was READY but Location Services were turned off"() {

        given:
        adapterWrapperMock.hasBluetoothAdapter() >> true
        adapterWrapperMock.isBluetoothEnabled() >> true
        locationServicesStatusMock.isLocationPermissionOk() >> true
        locationServicesOkSubject.onNext(true)
        objectUnderTest.subscribe(testSubscriber)

        when:
        testScheduler.triggerActions()

        then:
        testSubscriber.assertNoValues()

        when:
        locationServicesOkSubject.onNext(false)

        then:
        testSubscriber.assertValue(RxBleClient.State.LOCATION_SERVICES_NOT_ENABLED)
    }

    def "should emit BLUETOOTH_NOT_ENABLED state when was READY but Bluetooth was turned off"() {

        given:
        adapterWrapperMock.hasBluetoothAdapter() >> true
        adapterWrapperMock.isBluetoothEnabled() >> true
        locationServicesStatusMock.isLocationPermissionOk() >> true
        locationServicesOkSubject.onNext(true)
        objectUnderTest.subscribe(testSubscriber)

        when:
        testScheduler.triggerActions()

        then:
        testSubscriber.assertNoValues()

        when:
        adapterStateSubject.onNext(notOnAdapterState)

        then:
        testSubscriber.assertValue(RxBleClient.State.BLUETOOTH_NOT_ENABLED)

        where:
        notOnAdapterState << [
                STATE_OFF,
                STATE_TURNING_OFF,
                STATE_TURNING_ON
        ]
    }
}
