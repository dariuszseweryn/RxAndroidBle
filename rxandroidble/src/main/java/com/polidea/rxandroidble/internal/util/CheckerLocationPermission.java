package com.polidea.rxandroidble.internal.util;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;

import bleshadow.javax.inject.Inject;

public class CheckerLocationPermission {

    private final Context context;

    @Inject
    public CheckerLocationPermission(Context context) {
        this.context = context;
    }

    boolean isLocationPermissionGranted() {
        if (Math.min(Build.VERSION.SDK_INT, provideTargetSdk()) < 29) {
            return isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
                    || isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION);

        } else {
            return isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Copied from android.support.v4.content.ContextCompat for backwards compatibility
     * @param permission the permission to check
     * @return true is granted
     */
    private boolean isPermissionGranted(String permission) {
        if (permission == null) {
            throw new IllegalArgumentException("permission is null");
        }

        return context.checkPermission(permission, android.os.Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }

    private int provideTargetSdk() {
        try {
            return context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).targetSdkVersion;
        } catch (Throwable catchThemAll) {
            return Integer.MAX_VALUE;
        }
    }
}
