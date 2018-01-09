package com.polidea.rxandroidble.internal.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import org.robolectric.annotation.Config
import org.robospock.RoboSpecification
import rx.Subscription
import rx.observers.TestSubscriber

@Config(manifest = Config.NONE)
class LocationServicesOkObservableApi23Test extends RoboSpecification {
    def contextMock = Mock Context
    def mockLocationServicesStatus = Mock LocationServicesStatus
    def objectUnderTest = new LocationServicesOkObservableApi23(contextMock, mockLocationServicesStatus)
    BroadcastReceiver registeredReceiver

    def setup() {
        contextMock.getApplicationContext() >> contextMock
    }

    def "should register to correct receiver on subscribe"() {

        given:
        mockLocationServicesStatus.isLocationProviderOk() >> true

        when:
        objectUnderTest.subscribe()

        then:
        1 * contextMock.registerReceiver(!null, {
            it.hasAction("android.location.MODE_CHANGED")
        })
    }

    def "should unregister after observable was unsubscribed"() {

        given:
        mockLocationServicesStatus.isLocationProviderOk() >> true
        shouldCaptureRegisteredReceiver()
        Subscription subscription = objectUnderTest.subscribe()

        when:
        subscription.unsubscribe()

        then:
        1 * contextMock.unregisterReceiver(registeredReceiver)
    }

    def "should emit what LocationServicesStatus.isLocationProviderOk() returns on subscribe and on next broadcasts"() {

        given:
        shouldCaptureRegisteredReceiver()
        mockLocationServicesStatus.isLocationProviderOk() >>> [true, false, true]
        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>()

        when:
        objectUnderTest.subscribe(testSubscriber)

        then:
        testSubscriber.assertValue(true)

        when:
        postStateChangeBroadcast()

        then:
        testSubscriber.assertValues(true, false)

        when:
        postStateChangeBroadcast()

        then:
        testSubscriber.assertValues(true, false, true)
    }

    def "should not emit what LocationServicesStatus.isLocationProviderOk() returns on next broadcasts if the value does not change"() {

        given:
        shouldCaptureRegisteredReceiver()
        mockLocationServicesStatus.isLocationProviderOk() >>> [false, false, true, true, false, false]
        TestSubscriber<Boolean> testSubscriber = new TestSubscriber<>()

        when:
        objectUnderTest.subscribe(testSubscriber)

        then:
        testSubscriber.assertValue(false)

        when:
        postStateChangeBroadcast()

        then:
        testSubscriber.assertValue(false)

        when:
        postStateChangeBroadcast()

        then:
        testSubscriber.assertValues(false, true)

        when:
        postStateChangeBroadcast()

        then:
        testSubscriber.assertValues(false, true)

        when:
        postStateChangeBroadcast()

        then:
        testSubscriber.assertValues(false, true, false)

        when:
        postStateChangeBroadcast()

        then:
        testSubscriber.assertValues(false, true, false)
    }

    def postStateChangeBroadcast() {
        def intent = new Intent(LocationManager.PROVIDERS_CHANGED_ACTION)
        registeredReceiver.onReceive(contextMock, intent)
    }

    BroadcastReceiver shouldCaptureRegisteredReceiver() {
        _ * contextMock.registerReceiver({
            BroadcastReceiver receiver ->
                this.registeredReceiver = receiver
        }, _)
    }
}
