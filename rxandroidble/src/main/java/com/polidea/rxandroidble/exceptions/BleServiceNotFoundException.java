package com.polidea.rxandroidble.exceptions;

import java.util.UUID;

public class BleServiceNotFoundException extends BleException {

    private final UUID serviceUUID;

    public BleServiceNotFoundException(UUID serviceUUID) {
        super("BLE Service not found with uuid " + serviceUUID);
        this.serviceUUID = serviceUUID;
    }

    public UUID getServiceUUID() {
        return serviceUUID;
    }
}
