package com.polidea.rxandroidble.internal.connection;


import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.BluetoothGattCharacteristicProperty;
import com.polidea.rxandroidble.exceptions.BleIllegalOperationException;

/**
 * Handler for {@link IllegalOperationChecker#checkAnyPropertyMatches(BluetoothGattCharacteristic, int)} response.
 */
public abstract class IllegalOperationHandler {

    protected IllegalOperationMessageCreator messageCreator;

    IllegalOperationHandler(IllegalOperationMessageCreator messageCreator) {
        this.messageCreator = messageCreator;
    }

    public abstract @Nullable BleIllegalOperationException handleMismatchData(BluetoothGattCharacteristic characteristic,
                                                                              @BluetoothGattCharacteristicProperty int neededProperties);
}
