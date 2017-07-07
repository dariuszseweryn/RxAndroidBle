package com.polidea.rxandroidble.exceptions;

import android.support.annotation.RestrictTo;

import java.util.UUID;

/**
 * This exception is thrown when a non-supported operation has been requested upon a characteristic, eg. write operation on a
 * characteristic with only read property.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BleIllegalOperationException extends RuntimeException {

    public final UUID characteristicUUID;
    public final int supportedProperties;
    public final int neededProperties;

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