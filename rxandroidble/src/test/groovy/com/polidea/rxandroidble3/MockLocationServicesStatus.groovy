package com.polidea.rxandroidble3

import com.polidea.rxandroidble3.internal.util.LocationServicesStatus

class MockLocationServicesStatus implements LocationServicesStatus {
    boolean isLocationPermissionOk = true
    boolean isLocationProviderOk = true

    MockLocationServicesStatus() {
    }

    @Override
    boolean isLocationPermissionOk() {
        return isLocationPermissionOk
    }

    @Override
    boolean isLocationProviderOk() {
        return isLocationProviderOk
    }
}
