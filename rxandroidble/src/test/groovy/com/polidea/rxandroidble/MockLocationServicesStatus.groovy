package com.polidea.rxandroidble

import com.polidea.rxandroidble.internal.util.LocationServicesStatus

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
