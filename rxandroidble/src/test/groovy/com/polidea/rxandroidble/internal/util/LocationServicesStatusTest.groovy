package com.polidea.rxandroidble.internal.util

import android.os.Build
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class LocationServicesStatusTest extends Specification {

    def mockCheckerLocationProvider = Mock CheckerLocationProvider

    def mockCheckerLocationPermission = Mock CheckerLocationPermission

    def mockProviderDeviceSdk = Mock ProviderDeviceSdk

    def mockProviderApplicationTargetSdk = Mock ProviderApplicationTargetSdk

    def objectUnderTest = new LocationServicesStatus(mockCheckerLocationProvider, mockCheckerLocationPermission, mockProviderDeviceSdk, mockProviderApplicationTargetSdk)

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

    @Unroll
    def "(SDK <23) isLocationPermissionOk should return true (SDK=#sdkVersion)"() {

        given:
        mockProviderDeviceSdk.provide() >> sdkVersion

        expect:
        objectUnderTest.isLocationPermissionOk()

        where:
        sdkVersion << sdkVersionsPreM
    }

    @Unroll
    def "(SDK <23) isLocationPermissionOk should not call CheckerLocationPermission (SDK=#sdkVersion)"() {

        given:
        mockProviderDeviceSdk.provide() >> sdkVersion

        when:
        objectUnderTest.isLocationPermissionOk()

        then:
        0 * mockCheckerLocationPermission.isLocationPermissionGranted()

        where:
        sdkVersion << sdkVersionsPreM
    }

    @Unroll
    def "(SDK >=23) isLocationPermissionOk should return value from CheckerLocationPermission.isLocationPermissionGranted (permissionGranted:#permissionGranted SDK:#sdkVersion)"() {

        given:
        mockProviderDeviceSdk.provide() >> sdkVersion
        mockCheckerLocationPermission.isLocationPermissionGranted() >> permissionGranted

        expect:
        objectUnderTest.isLocationPermissionOk() == permissionGranted

        where:
        [sdkVersion, permissionGranted] << [sdkVersionsPostM, [true, false]].combinations()
    }

    @Unroll
    def "should check location provider only if needed (deviceSdk:#sdkVersion targetSdk:#targetSdk)"() {

        given:
        mockProviderDeviceSdk.provide() >> sdkVersion
        mockProviderApplicationTargetSdk.provide() >> targetSdk
        int expectedCalls
        if (sdkVersion >= Build.VERSION_CODES.M && targetSdk >= Build.VERSION_CODES.M) {
            expectedCalls = 1
        } else {
            expectedCalls = 0
        }

        when:
        objectUnderTest.isLocationProviderOk()

        then:
        expectedCalls * mockCheckerLocationProvider.isLocationProviderEnabled() >> true

        where:
        [sdkVersion, targetSdk] << [sdkVersions, sdkVersions].combinations()
    }
}
