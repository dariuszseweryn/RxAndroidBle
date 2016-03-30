package com.polidea.rxandroidble

import com.polidea.rxandroidble.internal.util.LocationServicesStatus

class MockLocationServicesStatus extends LocationServicesStatus {
    boolean isLocationPermissionApproved = true
    boolean isLocationProviderEnabled = true
    boolean isLocationProviderRequired = true

    MockLocationServicesStatus() {
        super(null, null)
    }

    @Override
    boolean isLocationPermissionApproved() {
        return isLocationPermissionApproved
    }

    @Override
    boolean isLocationProviderRequired() {
        return isLocationProviderRequired
    }

    @Override
    boolean isLocationProviderEnabled() {
        return isLocationProviderEnabled
    }
}
