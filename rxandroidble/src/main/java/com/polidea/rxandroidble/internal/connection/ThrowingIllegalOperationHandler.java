package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble.exceptions.BleIllegalOperationException;
import com.polidea.rxandroidble.internal.RxBleLog;

import javax.inject.Inject;

/**
 * Implementation of {@link IllegalOperationHandler}. This class logs an error and throws {@link BleIllegalOperationException} if there
 * was no match between possessed and requested properties.
 */
public class ThrowingIllegalOperationHandler extends IllegalOperationHandler {

    @Inject
    public ThrowingIllegalOperationHandler(IllegalOperationMessageCreator messageCreator) {
        super(messageCreator);
    }

    /**
     * This method logs an error and throws a {@link BleIllegalOperationException}.
     * @param characteristic the characteristic upon which the operation was requested
     * @param neededProperties bitmask of properties needed by the operation
     */
    @Override
    public void handleMismatchData(BluetoothGattCharacteristic characteristic, int neededProperties) {
        String message = messageCreator.createMismatchMessage(characteristic, neededProperties);
        RxBleLog.e(message);
        throw new BleIllegalOperationException(message,
                characteristic.getUuid(),
                characteristic.getProperties(),
                neededProperties);
    }
}
