package com.polidea.rxandroidble2.internal.util;

import com.polidea.rxandroidble2.ClientComponent;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

public class LocationServicesStatusApi23 implements LocationServicesStatus {

    private final CheckerLocationProvider checkerLocationProvider;
    private final CheckerScanPermission checkerScanPermission;
    private final boolean isAndroidWear;
    private final int targetSdk;
    private final int deviceSdk;

    @Inject
    LocationServicesStatusApi23(
            CheckerLocationProvider checkerLocationProvider,
            CheckerScanPermission checkerScanPermission,
            @Named(ClientComponent.PlatformConstants.INT_TARGET_SDK) int targetSdk,
            @Named(ClientComponent.PlatformConstants.INT_DEVICE_SDK) int deviceSdk,
            @Named(ClientComponent.PlatformConstants.BOOL_IS_ANDROID_WEAR) boolean isAndroidWear
    ) {
        this.checkerLocationProvider = checkerLocationProvider;
        this.checkerScanPermission = checkerScanPermission;
        this.targetSdk = targetSdk;
        this.deviceSdk = deviceSdk;
        this.isAndroidWear = isAndroidWear;
    }

    public boolean isLocationPermissionOk() {
        return checkerScanPermission.isScanRuntimePermissionGranted();
    }

    public boolean isLocationProviderOk() {
        return !isLocationProviderEnabledRequired() || checkerLocationProvider.isLocationProviderEnabled();
    }

    public boolean isScanPermissionOk() {
        return true;
    }

    public boolean isConnectPermissionOk() {
        return true;
    }

    /**
     * A function that returns true if the location services may be needed to be turned ON. Since there are no official guidelines
     * for Android Wear check is disabled.
     *
     * @return true if Location Services need to be turned ON
     * @see <a href="https://code.google.com/p/android/issues/detail?id=189090">Google Groups Discussion</a>
     */
    private boolean isLocationProviderEnabledRequired() {
        return !isAndroidWear && (
                // Apparently since device API 29 target SDK is not honored and location services need to be
                // turned on for the app to get scan results.
                // Based on issue https://github.com/Polidea/RxAndroidBle/issues/742
                deviceSdk >= 29 /* Build.VERSION_CODES.Q */
                        || targetSdk >= 23 /* Build.VERSION_CODES.M */
        );
    }
}
