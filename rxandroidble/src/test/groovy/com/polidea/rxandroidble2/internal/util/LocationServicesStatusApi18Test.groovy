package com.polidea.rxandroidble2.internal.util

import spock.lang.Specification

class LocationServicesStatusApi18Test extends Specification {

    LocationServicesStatusApi18 objectUnderTest

    private prepareObjectUnderTest() {
        objectUnderTest = new LocationServicesStatusApi18()
    }

    def "isLocationPermissionOk should return true"() {

        given:
        prepareObjectUnderTest()

        expect:
        objectUnderTest.isLocationPermissionOk()
    }

    def "isLocationProviderOk should return true"() {

        given:
        prepareObjectUnderTest()

        expect:
        objectUnderTest.isLocationProviderOk()
    }
}
