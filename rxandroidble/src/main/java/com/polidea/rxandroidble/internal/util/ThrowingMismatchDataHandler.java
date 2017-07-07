package com.polidea.rxandroidble.internal.util;

import com.polidea.rxandroidble.exceptions.BleIllegalOperationException;
import com.polidea.rxandroidble.internal.RxBleLog;

/**
 * Implementation of {@link MismatchDataHandler}. This class logs an error and throws {@link BleIllegalOperationException} if there
 * was no match between possessed and requested properties.
 */
public class ThrowingMismatchDataHandler implements MismatchDataHandler {

    /**
     * This method logs an error and throws a {@link BleIllegalOperationException}.
     * @param mismatchData container for information about mismatch
     */
    @Override
    public void handleMismatchData(IllegalOperationChecker.MismatchData mismatchData) {
        RxBleLog.e(mismatchData.message);
        throw new BleIllegalOperationException(mismatchData.message,
                mismatchData.uuid,
                mismatchData.supportedProperties,
                mismatchData.neededProperties);
    }
}
