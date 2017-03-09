package com.polidea.rxandroidble.internal.util;


import android.location.LocationManager;

public class CheckerLocationProvider {

    private final LocationManager locationManager;

    public CheckerLocationProvider(LocationManager locationManager) {
        this.locationManager = locationManager;
    }

    public boolean isLocationProviderEnabled() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
