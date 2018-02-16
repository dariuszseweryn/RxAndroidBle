package com.polidea.rxandroidble2.internal.connection;

import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble2.internal.BleIllegalOperationException;
import com.polidea.rxandroidble2.internal.RxBleLog;

import bleshadow.javax.inject.Inject;

/**
 * Implementation of {@link IllegalOperationHandler}. This class logs a warning if there was no match between possessed
 * and requested properties.
 */
public class LoggingIllegalOperationHandler extends IllegalOperationHandler {

    @Inject
    public LoggingIllegalOperationHandler(IllegalOperationMessageCreator messageCreator) {
        super(messageCreator);
    }

    /**
     * This method logs a warning.
     * @param characteristic the characteristic upon which the operation was requested
     * @param neededProperties bitmask of properties needed by the operation
     */
    @Override
    public BleIllegalOperationException handleMismatchData(BluetoothGattCharacteristic characteristic, int neededProperties) {
        RxBleLog.w(messageCreator.createMismatchMessage(characteristic, neededProperties));
        return null;
    }
}
