package com.polidea.rxandroidble2.internal.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import hkhc.electricspock.ElectricSpecification
import io.reactivex.schedulers.TestScheduler
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
class LocationServicesOkObservableApi23FactoryTest extends ElectricSpecification {
    def contextMock = Mock Context
    def mockLocationServicesStatus = Mock LocationServicesStatus
    def testScheduler = new TestScheduler()
    def objectUnderTest = new LocationServicesOkObservableApi23Factory(contextMock, mockLocationServicesStatus, testScheduler)
    BroadcastReceiver registeredReceiver

    def setup() {
        contextMock.getApplicationContext() >> contextMock
    }

    def "should register to correct receiver on subscribe"() {

        given:
        mockLocationServicesStatus.isLocationProviderOk() >> true
        objectUnderTest.get().subscribe()

        when:
        testScheduler.triggerActions()

        then:
        1 * contextMock.registerReceiver(!null, {
            it.hasAction("android.location.MODE_CHANGED")
        })
    }

    def "should unregister after observable was unsubscribed"() {

        given:
        mockLocationServicesStatus.isLocationProviderOk() >> true
        shouldCaptureRegisteredReceiver()
        def disposable = objectUnderTest.get().test()
        testScheduler.triggerActions()
        disposable.dispose()

        when:
        testScheduler.triggerActions()

        then:
        1 * contextMock.unregisterReceiver(registeredReceiver)
    }

    def "should still register and unregister in correct order"() {
        given:
        mockLocationServicesStatus.isLocationProviderOk() >> isLocationProviderOkResult
        objectUnderTest.get().take(1).test()

        when:
        testScheduler.triggerActions()

        then:
        1 * contextMock.registerReceiver(_, _)

        then:
        1 * contextMock.unregisterReceiver(_)

        where:
        isLocationProviderOkResult << [true, false]
    }

    def "should emit what LocationServicesStatus.isLocationProviderOk() returns on subscribe and on next broadcasts"() {

        given:
        shouldCaptureRegisteredReceiver()
        mockLocationServicesStatus.isLocationProviderOk() >>> [true, false, true]
        def testObserver = objectUnderTest.get().test()

        when:
        testScheduler.triggerActions()

        then:
        testObserver.assertValue(true)

        when:
        postStateChangeBroadcast()

        then:
        testObserver.assertValues(true, false)

        when:
        postStateChangeBroadcast()

        then:
        testObserver.assertValues(true, false, true)
    }

    def "should not emit what LocationServicesStatus.isLocationProviderOk() returns on next broadcasts if the value does not change"() {

        given:
        shouldCaptureRegisteredReceiver()
        mockLocationServicesStatus.isLocationProviderOk() >>> [false, false, true, true, false, false]
        def testObserver = objectUnderTest.get().test()

        when:
        testScheduler.triggerActions()

        then:
        testObserver.assertValue(false)

        when:
        postStateChangeBroadcast()

        then:
        testObserver.assertValue(false)

        when:
        postStateChangeBroadcast()

        then:
        testObserver.assertValues(false, true)

        when:
        postStateChangeBroadcast()

        then:
        testObserver.assertValues(false, true)

        when:
        postStateChangeBroadcast()

        then:
        testObserver.assertValues(false, true, false)

        when:
        postStateChangeBroadcast()

        then:
        testObserver.assertValues(false, true, false)
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
