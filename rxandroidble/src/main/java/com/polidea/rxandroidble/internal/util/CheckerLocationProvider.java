package com.polidea.rxandroidble.internal.util;


import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.location.LocationManager;

import android.os.Build;
import android.provider.Settings;
import bleshadow.javax.inject.Inject;

public class CheckerLocationProvider {

    private final ContentResolver contentResolver;
    private final LocationManager locationManager;

    @Inject
    public CheckerLocationProvider(ContentResolver contentResolver, LocationManager locationManager) {
        this.contentResolver = contentResolver;
        this.locationManager = locationManager;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public boolean isLocationProviderEnabled() {
        try {
            return Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF;
        } catch (Settings.SettingNotFoundException e) {
            return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
    }
}
