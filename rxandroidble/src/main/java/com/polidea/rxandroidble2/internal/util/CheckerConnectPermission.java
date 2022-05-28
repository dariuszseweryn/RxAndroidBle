package com.polidea.rxandroidble2.internal.util;


import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.ClientScope;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

@ClientScope
public class CheckerConnectPermission {

    private final CheckerPermission checkerPermission;
    private final String[][] connectPermissions;

    @Inject
    CheckerConnectPermission(
            CheckerPermission checkerPermission,
            @Named(ClientComponent.PlatformConstants.STRING_ARRAY_CONNECT_PERMISSIONS) String[][] connectPermissions
    ) {
        this.checkerPermission = checkerPermission;
        this.connectPermissions = connectPermissions;
    }

    public boolean isConnectRuntimePermissionGranted() {
        boolean allNeededPermissionsGranted = true;
        for (String[] neededPermissions : connectPermissions) {
            allNeededPermissionsGranted &= checkerPermission.isAnyPermissionGranted(neededPermissions);
        }
        return allNeededPermissionsGranted;
    }

    public String[] getRecommendedConnectRuntimePermissions() {
        int allPermissionsCount = 0;
        for (String[] permissionsArray : connectPermissions) {
            allPermissionsCount += permissionsArray.length;
        }
        String[] resultPermissions = new String[allPermissionsCount];
        int i = 0;
        for (String[] permissionsArray : connectPermissions) {
            for (String permission : permissionsArray) {
                resultPermissions[i++] = permission;
            }
        }
        return resultPermissions;
    }
}
