package com.polidea.rxandroidble2

import com.polidea.rxandroidble2.internal.util.LocationServicesStatus

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
