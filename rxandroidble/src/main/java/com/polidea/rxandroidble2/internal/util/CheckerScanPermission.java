package com.polidea.rxandroidble2.internal.util;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.ClientScope;

@ClientScope
public class CheckerScanPermission {

    private final Context context;
    private final String[][] scanPermissions;

    @Inject
    CheckerScanPermission(
            Context context,
            @Named(ClientComponent.PlatformConstants.STRING_ARRAY_SCAN_PERMISSIONS) String[][] scanPermissions
    ) {
        this.context = context;
        this.scanPermissions = scanPermissions;
    }

    public boolean isScanRuntimePermissionGranted() {
        boolean allNeededPermissionsGranted = true;
        for (String[] neededPermissions : scanPermissions) {
            allNeededPermissionsGranted &= isAnyPermissionGranted(neededPermissions);
        }
        return allNeededPermissionsGranted;
    }

    public boolean isLocationRuntimePermissionGranted() {
        return isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
                || isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public boolean isConnectRuntimePermissionGranted() {
        return isPermissionGranted(Manifest.permission.BLUETOOTH_CONNECT);
    }

    private boolean isAnyPermissionGranted(String[] acceptablePermissions) {
        for (String acceptablePermission : acceptablePermissions) {
            if (isPermissionGranted(acceptablePermission)) {
                return true;
            }
        }
        return false;
    }

    public String[] getRecommendedScanRuntimePermissions() {
        int allPermissionsCount = 0;
        for (String[] permissionsArray : scanPermissions) {
            allPermissionsCount += permissionsArray.length;
        }
        String[] resultPermissions = new String[allPermissionsCount];
        int i = 0;
        for (String[] permissionsArray : scanPermissions) {
            for (String permission : permissionsArray) {
                resultPermissions[i++] = permission;
            }
        }
        return resultPermissions;
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
