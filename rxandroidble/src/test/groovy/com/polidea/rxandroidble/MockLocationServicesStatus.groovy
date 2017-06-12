package com.polidea.rxandroidble

import com.polidea.rxandroidble.internal.util.LocationServicesStatus

class MockLocationServicesStatus extends LocationServicesStatus {
    boolean isLocationPermissionOk = true
    boolean isLocationProviderOk = true

    MockLocationServicesStatus() {
        super(null, null, 25, 25, false)
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
