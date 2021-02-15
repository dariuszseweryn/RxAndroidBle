package com.polidea.rxandroidble2.internal.connection;


import android.bluetooth.BluetoothGattCharacteristic;
import androidx.annotation.Nullable;

import com.polidea.rxandroidble2.internal.BluetoothGattCharacteristicProperty;
import com.polidea.rxandroidble2.internal.BleIllegalOperationException;

/**
 * Handler for {@link IllegalOperationChecker#checkAnyPropertyMatches(BluetoothGattCharacteristic, int)} response.
 */
public abstract class IllegalOperationHandler {

    protected final IllegalOperationMessageCreator messageCreator;

    IllegalOperationHandler(IllegalOperationMessageCreator messageCreator) {
        this.messageCreator = messageCreator;
    }

    public abstract @Nullable BleIllegalOperationException handleMismatchData(BluetoothGattCharacteristic characteristic,
                                                                              @BluetoothGattCharacteristicProperty int neededProperties);
}
