package com.polidea.rxandroidble.internal.util;

import com.polidea.rxandroidble.internal.RxBleLog;

/**
 * Implementation of {@link MismatchDataHandler}. This class logs a warning if there was no match between possessed
 * and requested properties.
 */
public class LoggingMismatchDataHandler implements MismatchDataHandler {

    /**
     * This method logs a warning.
     * @param mismatchData container for mismatch data
     */
    @Override
    public void handleMismatchData(IllegalOperationChecker.MismatchData mismatchData) {
        RxBleLog.w(mismatchData.message);
    }
}
