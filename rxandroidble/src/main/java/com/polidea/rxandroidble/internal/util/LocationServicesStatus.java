package com.polidea.rxandroidble.internal.util;

import android.os.Build;

import com.polidea.rxandroidble.ClientComponent;

import javax.inject.Inject;
import javax.inject.Named;

public class LocationServicesStatus {

    private final CheckerLocationProvider checkerLocationProvider;
    private final CheckerLocationPermission checkerLocationPermission;
    private final boolean isAndroidWear;
    private final int deviceSdk;
    private final int targetSdk;

    @Inject
    public LocationServicesStatus(
            CheckerLocationProvider checkerLocationProvider,
            CheckerLocationPermission checkerLocationPermission,
            @Named(ClientComponent.PlatformConstants.INT_DEVICE_SDK) int deviceSdk,
            @Named(ClientComponent.PlatformConstants.INT_TARGET_SDK) int targetSdk,
            @Named(ClientComponent.PlatformConstants.BOOL_IS_ANDROID_WEAR) boolean isAndroidWear
    ) {
        this.checkerLocationProvider = checkerLocationProvider;
        this.checkerLocationPermission = checkerLocationPermission;
        this.deviceSdk = deviceSdk;
        this.targetSdk = targetSdk;
        this.isAndroidWear = isAndroidWear;
    }

    public boolean isLocationPermissionOk() {
        return !isLocationPermissionGrantedRequired() || checkerLocationPermission.isLocationPermissionGranted();
    }

    public boolean isLocationProviderOk() {
        return !isLocationProviderEnabledRequired() || checkerLocationProvider.isLocationProviderEnabled();
    }

    private boolean isLocationPermissionGrantedRequired() {
        return deviceSdk >= Build.VERSION_CODES.M;
    }

    /**
     * A function that returns true if the location services may be needed to be turned ON. Since there are no official guidelines
     * for Android Wear check is disabled.
     *
     * @see <a href="https://code.google.com/p/android/issues/detail?id=189090">Google Groups Discussion</a>
     * @return true if Location Services need to be turned ON
     */
    private boolean isLocationProviderEnabledRequired() {
        return !isAndroidWear
                && targetSdk >= Build.VERSION_CODES.M
                && deviceSdk >= Build.VERSION_CODES.M;
    }
}
