package com.polidea.rxandroidble.internal.connection;

import com.polidea.rxandroidble.exceptions.BleIllegalOperationException;
import com.polidea.rxandroidble.internal.RxBleLog;

import java.util.UUID;

/**
 * Implementation of {@link IllegalOperationHandler}. This class logs an error and throws {@link BleIllegalOperationException} if there
 * was no match between possessed and requested properties.
 */
public class ThrowingIllegalOperationHandler implements IllegalOperationHandler {


    /**
     * This method logs an error and throws a {@link BleIllegalOperationException}.
     * @param message message to be displayed in log and exception
     * @param characteristicUuid UUID of the characteristic upon which the operation was requested
     * @param supportedProperties bitmask of properties supported by the characteristic
     * @param neededProperties bitmask of properties needed by the operation
     */
    @Override
    public void handleMismatchData(String message, UUID characteristicUuid, int supportedProperties, int neededProperties) {
        RxBleLog.e(message);
        throw new BleIllegalOperationException(message,
                characteristicUuid,
                supportedProperties,
                neededProperties);
    }
}
