package com.polidea.rxandroidble2.internal.util;


public interface LocationServicesStatus {

    boolean isLocationPermissionOk();
    boolean isLocationProviderOk();
    boolean isScanPermissionOk();
    boolean isConnectPermissionOk();
}
