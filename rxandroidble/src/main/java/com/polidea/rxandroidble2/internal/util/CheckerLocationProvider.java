package com.polidea.rxandroidble2.internal.util;


import android.content.ContentResolver;
import android.location.LocationManager;

import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

import bleshadow.javax.inject.Inject;

import com.polidea.rxandroidble2.internal.RxBleLog;

public class CheckerLocationProvider {

    private final ContentResolver contentResolver;
    private final LocationManager locationManager;

    @Inject
    CheckerLocationProvider(ContentResolver contentResolver, LocationManager locationManager) {
        this.contentResolver = contentResolver;
        this.locationManager = locationManager;
    }

    public boolean isLocationProviderEnabled() {
        if (Build.VERSION.SDK_INT >= 28 /* Build.VERSION_CODES.P */) {
            return locationManager.isLocationEnabled();
        }
        if (Build.VERSION.SDK_INT >= 19 /* Build.VERSION_CODES.KITKAT */) {
            return isLocationProviderEnabledBelowApi28();
        }
        return isLocationProviderEnabledBelowApi19();
    }

    // Android Studio does not see the deprecation of Settings.Secure.LOCATION_MODE in this method (if you the same code it to
    // `isLocationProviderEnabled` then AS will properly see it). On the other hand Gradle will complain that a deprecated code is used.
    // To silence Gradle warning a @SuppressWarnings is needed. @SuppressWarnings triggers a warning in AS as a redundant suppression.
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    @RequiresApi(19 /* Build.VERSION_CODES.KITKAT */)
    private boolean isLocationProviderEnabledBelowApi28() {
        try {
            return Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF;
        } catch (Settings.SettingNotFoundException e) {
            RxBleLog.w(e, "Could not use LOCATION_MODE check. Falling back to a legacy/heuristic function.");
            return isLocationProviderEnabledBelowApi19();
        }
    }

    private boolean isLocationProviderEnabledBelowApi19() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
