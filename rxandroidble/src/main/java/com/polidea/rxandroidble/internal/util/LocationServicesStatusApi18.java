package com.polidea.rxandroidble.internal.util;

import bleshadow.javax.inject.Inject;

public class LocationServicesStatusApi18 implements LocationServicesStatus {

    @Inject
    LocationServicesStatusApi18() {

    }

    public boolean isLocationPermissionOk() {
        return true;
    }

    public boolean isLocationProviderOk() {
        return true;
    }
}
