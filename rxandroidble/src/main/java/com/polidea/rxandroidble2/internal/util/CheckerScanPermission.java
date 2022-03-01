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
    private final String[][] connectPermissions;

    @Inject
    CheckerScanPermission(
            Context context,
            @Named(ClientComponent.PlatformConstants.STRING_ARRAY_SCAN_PERMISSIONS) String[][] scanPermissions,
            @Named(ClientComponent.PlatformConstants.STRING_ARRAY_CONNECT_PERMISSIONS) String[][] connectPermissions
    ) {
        this.context = context;
        this.scanPermissions = scanPermissions;
        this.connectPermissions = connectPermissions;
    }

    public boolean isScanRuntimePermissionGranted() {
        return isAllPermissionsGranted(scanPermissions);
    }

    public boolean isConnectRuntimePermissionGranted() {
        return isAllPermissionsGranted(connectPermissions);
    }

    public boolean isLocationRuntimePermissionGranted() {
        return isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
                || isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private boolean isAllPermissionsGranted(String[][] neededPermissions) {
        boolean allNeededPermissionsGranted = true;
        for (String[] permissions : neededPermissions) {
            allNeededPermissionsGranted &= isAnyPermissionGranted(permissions);
        }
        return allNeededPermissionsGranted;
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
        return getRecommendedRuntimePermissions(scanPermissions);
    }

    public String[] getRecommendedConnectRuntimePermissions() {
        return getRecommendedRuntimePermissions(connectPermissions);
    }

    private String[] getRecommendedRuntimePermissions(String[][] permissions) {
        int allPermissionsCount = 0;
        for (String[] permissionsArray : permissions) {
            allPermissionsCount += permissionsArray.length;
        }
        String[] resultPermissions = new String[allPermissionsCount];
        int i = 0;
        for (String[] permissionsArray : permissions) {
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
