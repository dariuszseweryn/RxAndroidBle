package com.polidea.rxandroidble.internal.util;

import android.annotation.TargetApi;
import android.os.Build;

public class LocationServicesStatus {

    private final CheckerLocationProvider checkerLocationProvider;

    private final CheckerLocationPermission checkerLocationPermission;

    private final ProviderDeviceSdk providerDeviceSdk;

    private final ProviderApplicationTargetSdk providerApplicationTargetSdk;

    public LocationServicesStatus(
            CheckerLocationProvider checkerLocationProvider,
            CheckerLocationPermission checkerLocationPermission,
            ProviderDeviceSdk providerDeviceSdk,
            ProviderApplicationTargetSdk providerApplicationTargetSdk
    ) {
        this.checkerLocationProvider = checkerLocationProvider;
        this.checkerLocationPermission = checkerLocationPermission;
        this.providerDeviceSdk = providerDeviceSdk;
        this.providerApplicationTargetSdk = providerApplicationTargetSdk;
    }

    public boolean isLocationPermissionOk() {
        return !isLocationPermissionGrantedRequired() || isLocationPermissionGranted();
    }

    public boolean isLocationProviderOk() {
        return !isLocationProviderEnabledRequired() || isLocationProviderEnabled();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean isLocationPermissionGranted() {
        return checkerLocationPermission.isLocationPermissionGranted();
    }

    private boolean isLocationProviderEnabled() {
        return checkerLocationProvider.isLocationProviderEnabled();
    }

    private boolean isLocationPermissionGrantedRequired() {
        return providerDeviceSdk.provide() >= Build.VERSION_CODES.M;
    }

    /**
     * A function that returns true if the location services may be needed to be turned ON.
     *
     * @see <a href="https://code.google.com/p/android/issues/detail?id=189090">Google Groups Discussion</a>
     * @return true if Location Services need to be turned ON
     */
    private boolean isLocationProviderEnabledRequired() {
        return providerApplicationTargetSdk.provide() >= Build.VERSION_CODES.M
                && providerDeviceSdk.provide() >= Build.VERSION_CODES.M;
    }
}
