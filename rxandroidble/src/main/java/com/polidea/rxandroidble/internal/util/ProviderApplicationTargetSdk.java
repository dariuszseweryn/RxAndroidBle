package com.polidea.rxandroidble.internal.util;


import android.content.Context;

public class ProviderApplicationTargetSdk {

    private final int targetSdkVersion;

    public ProviderApplicationTargetSdk(Context context) {
        int tempTargetSdkVersion;
        try {
            tempTargetSdkVersion = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0).targetSdkVersion;
        } catch (Throwable catchThemAll) {
            tempTargetSdkVersion = Integer.MAX_VALUE;
        }
        targetSdkVersion = tempTargetSdkVersion;
    }

    public int provide() {
        return targetSdkVersion;
    }
}
