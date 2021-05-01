package com.polidea.rxandroidble2.internal.util;


import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.location.LocationManager;

import android.os.Build;
import android.provider.Settings;
import bleshadow.javax.inject.Inject;
import com.polidea.rxandroidble2.internal.RxBleLog;

@TargetApi(19 /* Build.VERSION_CODES.KITKAT */)
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
            try {
                //noinspection deprecation
                return Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF;
            } catch (Settings.SettingNotFoundException e) {
                RxBleLog.w(e, "Could not use LOCATION_MODE check. Falling back to a legacy/heuristic function.");
            }
        }
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
