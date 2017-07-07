package com.polidea.rxandroidble.internal.connection;


import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

/**
 * Handler for {@link IllegalOperationChecker#checkAnyPropertyMatches(BluetoothGattCharacteristic, int)} response.
 */
public interface IllegalOperationHandler {
    void handleMismatchData(String message, UUID characteristicUuid, int supportedProperties, int neededProperties);
}
