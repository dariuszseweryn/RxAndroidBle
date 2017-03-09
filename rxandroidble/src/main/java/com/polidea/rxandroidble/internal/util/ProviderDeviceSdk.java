package com.polidea.rxandroidble.internal.util;


import android.os.Build;

public class ProviderDeviceSdk {

    public int provide() {
        return Build.VERSION.SDK_INT;
    }
}
