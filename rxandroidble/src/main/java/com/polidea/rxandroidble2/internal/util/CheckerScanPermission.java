package com.polidea.rxandroidble2.internal.util;


import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.ClientScope;
import com.polidea.rxandroidble2.internal.RxBleLog;

import java.util.Arrays;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

@ClientScope
public class CheckerScanPermission {

    private final CheckerPermission checkerPermission;
    private final String[][] scanPermissions;

    @Inject
    CheckerScanPermission(
            CheckerPermission checkerPermission,
            @Named(ClientComponent.PlatformConstants.STRING_ARRAY_SCAN_PERMISSIONS) String[][] scanPermissions
    ) {
        this.checkerPermission = checkerPermission;
        this.scanPermissions = scanPermissions;
    }

    public boolean isScanRuntimePermissionGranted() {
        boolean allNeededPermissionsGranted = true;
        for (String[] neededPermissions : scanPermissions) {
            allNeededPermissionsGranted &= checkerPermission.isAnyPermissionGranted(neededPermissions);
        }
        RxBleLog.d("Scan Runtime Permission Granted: %s", allNeededPermissionsGranted);
        return allNeededPermissionsGranted;
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
        RxBleLog.d("Recommended Scan Runtime Permissions: %s", Arrays.toString(resultPermissions));
        return resultPermissions;
    }
}
