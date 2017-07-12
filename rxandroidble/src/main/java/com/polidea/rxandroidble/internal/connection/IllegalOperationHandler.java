package com.polidea.rxandroidble.internal.connection;


import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Handler for {@link IllegalOperationChecker#checkAnyPropertyMatches(BluetoothGattCharacteristic, int)} response.
 */
public abstract class IllegalOperationHandler {

    protected IllegalOperationMessageCreator messageCreator;

    IllegalOperationHandler(IllegalOperationMessageCreator messageCreator) {
        this.messageCreator = messageCreator;
    }

    public abstract void handleMismatchData(BluetoothGattCharacteristic characteristic, int neededProperties);
}
