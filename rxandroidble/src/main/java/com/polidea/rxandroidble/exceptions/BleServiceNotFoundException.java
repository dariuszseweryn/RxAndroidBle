package com.polidea.rxandroidble.exceptions;

import java.util.UUID;

public class BleServiceNotFoundException extends BleException {

    private final UUID serviceUUID;

    public BleServiceNotFoundException(UUID serviceUUID) {
        this.serviceUUID = serviceUUID;
    }

    public UUID getServiceUUID() {
        return serviceUUID;
    }

    @Override
    public String toString() {
        return "BleServiceNotFoundException{" + "serviceUUID=" + serviceUUID + '}';
    }
}
