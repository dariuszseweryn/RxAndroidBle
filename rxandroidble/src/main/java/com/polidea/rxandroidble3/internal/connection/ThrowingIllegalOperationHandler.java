package com.polidea.rxandroidble2.internal.connection;

import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble2.internal.BleIllegalOperationException;

import bleshadow.javax.inject.Inject;

/**
 * Implementation of {@link IllegalOperationHandler}. This class logs an error and returns {@link BleIllegalOperationException} if there
 * was no match between possessed and requested properties.
 */
public class ThrowingIllegalOperationHandler extends IllegalOperationHandler {

    @Inject
    public ThrowingIllegalOperationHandler(IllegalOperationMessageCreator messageCreator) {
        super(messageCreator);
    }

    /**
     * This method logs an error and returns a {@link BleIllegalOperationException}.
     * @param characteristic the characteristic upon which the operation was requested
     * @param neededProperties bitmask of properties needed by the operation
     */
    @Override
    public BleIllegalOperationException handleMismatchData(BluetoothGattCharacteristic characteristic, int neededProperties) {
        String message = messageCreator.createMismatchMessage(characteristic, neededProperties);
        return new BleIllegalOperationException(message,
                characteristic.getUuid(),
                characteristic.getProperties(),
                neededProperties);
    }
}
