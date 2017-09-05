package com.polidea.rxandroidble.internal.connection

import static com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState.STATE_OFF
import static com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_OFF
import static com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState.STATE_TURNING_ON

import com.polidea.rxandroidble.RxBleAdapterStateObservable
import com.polidea.rxandroidble.exceptions.BleDisconnectedException
import com.polidea.rxandroidble.exceptions.BleException
import org.robospock.RoboSpecification
import rx.observers.TestSubscriber
import rx.subjects.PublishSubject
import spock.lang.Unroll

class DisconnectionRouterTest extends RoboSpecification {

    String mockMacAddress = "1234"
    PublishSubject<RxBleAdapterStateObservable.BleAdapterState> mockAdapterStateSubject = PublishSubject.create()
    DisconnectionRouter objectUnderTest = new DisconnectionRouter(mockMacAddress, mockAdapterStateSubject)
    TestSubscriber testSubscriber = new TestSubscriber()

    def "should emit exception from .asObservable() when got one from .route()"() {

        given:
        BleException testException = new BleException()
        objectUnderTest.asObservable().subscribe(testSubscriber)

        when:
        objectUnderTest.route(testException)

        then:
        testSubscriber.assertError(testException)
    }

    def "should emit exception from .asObservable() when got one from .route() even before subscription"() {

        given:
        BleException testException = new BleException()
        objectUnderTest.route(testException)

        when:
        objectUnderTest.asObservable().subscribe(testSubscriber)

        then:
        testSubscriber.assertError(testException)
    }

    @Unroll
    def "should emit exception from .asObservable() when adapterStateObservable emits STATE_TURNING_ON/STATE_TURNING_OFF/STATE_OFF"() {

        given:
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
        mockAdapterStateSubject.onNext(bleAdapterState)

        when:
        objectUnderTest.asObservable().subscribe(testSubscriber)

        then:
        testSubscriber.assertError({ BleDisconnectedException e -> e.bluetoothDeviceAddress == mockMacAddress })

        where:
        bleAdapterState << [ STATE_TURNING_ON, STATE_TURNING_OFF, STATE_OFF ]
    }

    def "should not emit exception from .asObservable() when adapterStateObservable emits STATE_ON"() {

        given:
        objectUnderTest.asObservable().subscribe(testSubscriber)

        when:
        mockAdapterStateSubject.onNext(RxBleAdapterStateObservable.BleAdapterState.STATE_ON)

        then:
        testSubscriber.assertNoErrors()
    }
}
