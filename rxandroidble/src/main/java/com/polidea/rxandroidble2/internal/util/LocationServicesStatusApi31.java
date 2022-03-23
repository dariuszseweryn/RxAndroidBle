package com.polidea.rxandroidble2.internal.util;

import android.annotation.TargetApi;

import com.polidea.rxandroidble2.ClientComponent;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

@TargetApi(31 /* Build.VERSION_CODES.S */)
public class LocationServicesStatusApi31 implements LocationServicesStatus {

    private final CheckerLocationProvider checkerLocationProvider;
    private final CheckerScanPermission checkerScanPermission;
    private final boolean isAndroidWear;
    private final boolean isNearbyPermissionNeverForLoc;

    @Inject
    LocationServicesStatusApi31(
            CheckerLocationProvider checkerLocationProvider,
            CheckerScanPermission checkerScanPermission,
            @Named(ClientComponent.PlatformConstants.BOOL_IS_ANDROID_WEAR) boolean isAndroidWear,
            @Named(ClientComponent.PlatformConstants.BOOL_IS_NEARBY_PERMISSION_NEVER_FOR_LOCATION) boolean isNearbyPermissionNeverForLoc
    ) {
        this.checkerLocationProvider = checkerLocationProvider;
        this.checkerScanPermission = checkerScanPermission;
        this.isAndroidWear = isAndroidWear;
        this.isNearbyPermissionNeverForLoc = isNearbyPermissionNeverForLoc;
    }

    public boolean isLocationPermissionOk() {
        return isNearbyPermissionNeverForLoc || checkerScanPermission.isFineLocationRuntimePermissionGranted();
    }

    public boolean isLocationProviderOk() {
        return !isLocationProviderEnabledRequired() || checkerLocationProvider.isLocationProviderEnabled();
    }

    @Override
    public boolean isScanPermissionOk() {
        return checkerScanPermission.isScanRuntimePermissionGranted();
    }

    public boolean isConnectPermissionOk() {
        return checkerScanPermission.isConnectRuntimePermissionGranted();
    }

    /**
     * A function that returns true if the location services may be needed to be turned ON. Since there are no official guidelines
     * for Android Wear check is disabled.
     *
     * @return true if Location Services need to be turned ON
     * @see <a href="https://code.google.com/p/android/issues/detail?id=189090">Google Groups Discussion</a>
     */
    private boolean isLocationProviderEnabledRequired() {
        if (isAndroidWear) {
            return false;
        }
        return !isNearbyPermissionNeverForLoc;
    }
}
