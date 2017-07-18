package com.polidea.rxandroidble.exceptions;

import android.support.annotation.RestrictTo;

import com.polidea.rxandroidble.BluetoothGattCharacteristicProperty;

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
                                        int supportedProperties,
                                        int neededProperties) {
        super(message);
        this.characteristicUUID = characteristicUUID;
        this.supportedProperties = supportedProperties;
        this.neededProperties = neededProperties;
    }
}