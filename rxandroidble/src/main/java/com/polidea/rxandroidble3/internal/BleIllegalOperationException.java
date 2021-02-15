package com.polidea.rxandroidble2.internal;

import androidx.annotation.RestrictTo;

import java.util.UUID;

/**
 * This exception is thrown when a non-supported operation has been requested upon a characteristic, eg. write operation on a
 * characteristic with only read property.
 */
public class BleIllegalOperationException extends RuntimeException {

    public final UUID characteristicUUID;
    public final @BluetoothGattCharacteristicProperty int supportedProperties;
    public final @BluetoothGattCharacteristicProperty int neededProperties;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public BleIllegalOperationException(String message,
                                        UUID characteristicUUID,
                                        @BluetoothGattCharacteristicProperty int supportedProperties,
                                        @BluetoothGattCharacteristicProperty int neededProperties) {
        super(message);
        this.characteristicUUID = characteristicUUID;
        this.supportedProperties = supportedProperties;
        this.neededProperties = neededProperties;
    }
}