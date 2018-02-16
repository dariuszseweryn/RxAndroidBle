package com.polidea.rxandroidble2.internal.util

import android.os.Build
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class LocationServicesStatusApi23Test extends Specification {

    def mockCheckerLocationProvider = Mock CheckerLocationProvider
    def mockCheckerLocationPermission = Mock CheckerLocationPermission
    int mockApplicationTargetSdk
    boolean mockIsAndroidWear
    LocationServicesStatusApi23 objectUnderTest

    private prepareObjectUnderTest() {
        objectUnderTest = new LocationServicesStatusApi23(mockCheckerLocationProvider, mockCheckerLocationPermission, mockApplicationTargetSdk, mockIsAndroidWear)
    }

    @Shared
    private def sdkVersionsPreM = [
            Build.VERSION_CODES.JELLY_BEAN_MR2,
            Build.VERSION_CODES.KITKAT,
            Build.VERSION_CODES.KITKAT_WATCH,
            Build.VERSION_CODES.LOLLIPOP,
            Build.VERSION_CODES.LOLLIPOP_MR1,
    ]

    @Shared
    private def sdkVersionsPostM = [
            Build.VERSION_CODES.M,
            Build.VERSION_CODES.N,
            Build.VERSION_CODES.CUR_DEVELOPMENT,
    ]

    @Shared
    private def sdkVersions = sdkVersionsPreM + sdkVersionsPostM

    @Shared
    private def isAndroidWear = [true, false]

    @Unroll
    def "isLocationPermissionOk should return value from CheckerLocationPermission.isLocationPermissionGranted (permissionGranted:#permissionGranted)"() {

        given:
        prepareObjectUnderTest()
        mockCheckerLocationPermission.isLocationPermissionGranted() >> permissionGranted

        expect:
        objectUnderTest.isLocationPermissionOk() == permissionGranted

        where:
        permissionGranted << [true, false]
    }

    @Unroll
    def "should check location provider only if needed (targetSdk:#targetSdk isAndroidWear:#isAndroidWearValue)"() {

        given:
        mockApplicationTargetSdk = targetSdk
        mockIsAndroidWear = isAndroidWearValue
        prepareObjectUnderTest()
        int expectedCalls
        if (targetSdk >= Build.VERSION_CODES.M && !isAndroidWearValue) {
            expectedCalls = 1
        } else {
            expectedCalls = 0
        }

        when:
        objectUnderTest.isLocationProviderOk()

        then:
        expectedCalls * mockCheckerLocationProvider.isLocationProviderEnabled() >> true

        where:
        [targetSdk, isAndroidWearValue] << [sdkVersions, isAndroidWear].combinations()
    }
}
