package com.polidea.rxandroidble2.exceptions;


import android.Manifest;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class BlePermissionException extends BleException {

    @StringDef({Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Permission {
    }

    public BlePermissionException(@Permission String permission) {
        super(createMessage(permission));
    }

    private static String createMessage(@Permission String permission) {
        return "BLE permission exception: " + permission;
    }
}
