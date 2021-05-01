package com.polidea.rxandroidble2.internal.util

import android.os.Build
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class LocationServicesStatusApi23Test extends Specification {

    def mockCheckerLocationProvider = Mock CheckerLocationProvider
    def mockCheckerLocationPermission = Mock CheckerLocationPermission
    int mockApplicationTargetSdk
    int mockApplicationDeviceSdk
    boolean mockIsAndroidWear
    LocationServicesStatusApi23 objectUnderTest

    private prepareObjectUnderTest() {
        objectUnderTest = new LocationServicesStatusApi23(mockCheckerLocationProvider, mockCheckerLocationPermission, mockApplicationTargetSdk, mockApplicationDeviceSdk, mockIsAndroidWear)
    }

    // API 18 is the first version with official AOSP BLE, API 21 is the last w/o need of location
    @Shared
    private def targetSdkVersionsWithoutLocationNeed = [Build.VERSION_CODES.JELLY_BEAN_MR2, Build.VERSION_CODES.LOLLIPOP_MR1]

    // API 22 is the first that needs location, API 28 is the last that deviceSdk that does not need location if targetSdk < API 22, API 29 needs location regardless of targetSdk
    @Shared
    private def targetSdkVersionsWithLocationNeed = [Build.VERSION_CODES.M, Build.VERSION_CODES.P, Build.VERSION_CODES.Q, Build.VERSION_CODES.CUR_DEVELOPMENT]

    @Shared
    private def interestingVersions = targetSdkVersionsWithoutLocationNeed + targetSdkVersionsWithLocationNeed

    @Shared
    private def isAndroidWear = [true, false]

    @Unroll
    def "isLocationPermissionOk should return value from CheckerLocationPermission.areScanPermissionsOk (permissionGranted:#permissionGranted)"() {

        given:
        prepareObjectUnderTest()
        mockCheckerLocationPermission.isScanRuntimePermissionGranted() >> permissionGranted

        expect:
        objectUnderTest.isLocationPermissionOk() == permissionGranted

        where:
        permissionGranted << [true, false]
    }

    @Unroll
    def "should check location provider only if needed (deviceSdk:#deviceSdk targetSdk:#targetSdk isAndroidWear:#isAndroidWearValue)"() {

        given:
        mockApplicationTargetSdk = targetSdk
        mockApplicationDeviceSdk = deviceSdk
        mockIsAndroidWear = isAndroidWearValue
        prepareObjectUnderTest()
        int expectedCalls
        if (isAndroidWearValue) {
            expectedCalls = 0
        } else if (deviceSdk >= Build.VERSION_CODES.Q) {
            expectedCalls = 1
        } else if (targetSdk >= Build.VERSION_CODES.M) {
            expectedCalls = 1
        } else {
            expectedCalls = 0
        }

        when:
        objectUnderTest.isLocationProviderOk()

        then:
        expectedCalls * mockCheckerLocationProvider.isLocationProviderEnabled() >> true

        where:
        [targetSdk, deviceSdk, isAndroidWearValue] << [interestingVersions, interestingVersions, isAndroidWear].combinations()
    }
}
