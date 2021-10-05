package com.polidea.rxandroidble2.internal.util;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.internal.RxBleLog;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

@TargetApi(31 /* Build.VERSION_CODES.S */)
public class LocationServicesStatusApi31 implements LocationServicesStatus {

    private final CheckerLocationProvider checkerLocationProvider;
    private final CheckerScanPermission checkerScanPermission;
    private final Context context;
    private final boolean isAndroidWear;

    @Inject
    LocationServicesStatusApi31(
            CheckerLocationProvider checkerLocationProvider,
            CheckerScanPermission checkerScanPermission,
            Context context,
            @Named(ClientComponent.PlatformConstants.BOOL_IS_ANDROID_WEAR) boolean isAndroidWear
    ) {
        this.checkerLocationProvider = checkerLocationProvider;
        this.checkerScanPermission = checkerScanPermission;
        this.context = context;
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
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
                if (!Manifest.permission.BLUETOOTH_SCAN.equals(packageInfo.requestedPermissions[i])) {
                    continue;
                }
                return (packageInfo.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_NEVER_FOR_LOCATION) == 0;
            }
        } catch (PackageManager.NameNotFoundException e) {
            RxBleLog.e(e, "Could not find application PackageInfo");
        }
        // default to a safe value
        return true;
    }
}
