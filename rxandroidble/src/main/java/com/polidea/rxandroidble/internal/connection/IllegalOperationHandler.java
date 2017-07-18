package com.polidea.rxandroidble.internal.connection;


import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble.exceptions.BleIllegalOperationException;

/**
 * Handler for {@link IllegalOperationChecker#checkAnyPropertyMatches(BluetoothGattCharacteristic, int)} response.
 */
public abstract class IllegalOperationHandler {

    protected IllegalOperationMessageCreator messageCreator;

    IllegalOperationHandler(IllegalOperationMessageCreator messageCreator) {
        this.messageCreator = messageCreator;
    }

    public abstract BleIllegalOperationException handleMismatchData(BluetoothGattCharacteristic characteristic, int neededProperties);
}
