package com.polidea.rxandroidble.internal.util;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;

public class LocationServicesStatus {

    private final Context context;
    private final LocationManager locationManager;

    public LocationServicesStatus(Context context, LocationManager locationManager) {
        this.context = context;
        this.locationManager = locationManager;
    }

    public boolean isLocationPermissionApproved() {
        return isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
                || isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public boolean isLocationProviderEnabled() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean isLocationProviderRequired() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean isPermissionGranted(String permission) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }
}
