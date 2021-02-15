package com.polidea.rxandroidble2.exceptions;

import java.util.UUID;

/**
 * An exception emitted from {@link com.polidea.rxandroidble2.RxBleDeviceServices} or {@link com.polidea.rxandroidble2.RxBleConnection}
 * functions that take service's {@link UUID} as a param in case the service with the corresponding UUID is not found in the discovered
 * services.
 */
public class BleServiceNotFoundException extends BleException {

    private final UUID serviceUUID;

    public BleServiceNotFoundException(UUID serviceUUID) {
        super("BLE Service not found with UUID " + serviceUUID);
        this.serviceUUID = serviceUUID;
    }

    public UUID getServiceUUID() {
        return serviceUUID;
    }
}
