package com.polidea.rxandroidble2.internal.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

import com.polidea.rxandroidble2.ClientScope;

import bleshadow.javax.inject.Inject;

@ClientScope
public class CheckerPermission {

    private final Context context;

    @Inject
    CheckerPermission(Context context) {
        this.context = context;
    }

    boolean isAnyPermissionGranted(String[] acceptablePermissions) {
        for (String acceptablePermission : acceptablePermissions) {
            if (isPermissionGranted(acceptablePermission)) {
                return true;
            }
        }
        return false;
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

        return context.checkPermission(permission, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }
}
