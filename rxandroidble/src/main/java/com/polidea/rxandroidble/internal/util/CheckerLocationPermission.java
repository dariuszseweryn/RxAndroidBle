package com.polidea.rxandroidble.internal.util;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

import javax.inject.Inject;

public class CheckerLocationPermission {

    private final Context context;

    @Inject
    public CheckerLocationPermission(Context context) {
        this.context = context;
    }

    boolean isLocationPermissionGranted() {
        return isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
                || isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION);
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
}
