package com.polidea.rxandroidble2.internal.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.polidea.rxandroidble2.ClientComponent;

import java.util.ArrayList;

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
        if (targetSdk >= 31 /* Build.VERSION_CODES.S */ && deviceSdk >= 31 /* Build.VERSION_CODES.S */) {
            Context c;
            PackageInfo packageInfo = c.getPackageManager().getPackageInfo(c.getPackageName(), PackageManager.GET_PERMISSIONS);
            for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
                if (!Manifest.permission.BLUETOOTH_SCAN.equals(packageInfo.requestedPermissions[i])) {
                    continue;
                }
                return (packageInfo.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_NEVER_FOR_LOCATION) == 0;
            }
            throw new RuntimeException("!!");
        }
        // Apparently since device API 29 target SDK is not honored and location services need to be
        // turned on for the app to get scan results.
        // Based on issue https://github.com/Polidea/RxAndroidBle/issues/742
        return deviceSdk >= 29 /* Build.VERSION_CODES.Q */
                || targetSdk >= 23 /* Build.VERSION_CODES.M */;
    }
}
