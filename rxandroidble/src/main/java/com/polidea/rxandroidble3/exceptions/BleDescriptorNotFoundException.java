package com.polidea.rxandroidble2.exceptions;

import java.util.UUID;

public class BleDescriptorNotFoundException extends BleException {

    private final UUID descriptorUUID;

    public BleDescriptorNotFoundException(UUID descriptorUUID) {
        super("Descriptor not found with UUID " + descriptorUUID);
        this.descriptorUUID = descriptorUUID;
    }

    public UUID getDescriptorUUID() {
        return descriptorUUID;
    }
}
