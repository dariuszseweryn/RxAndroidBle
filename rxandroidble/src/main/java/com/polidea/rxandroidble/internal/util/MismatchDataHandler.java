package com.polidea.rxandroidble.internal.util;


/**
 * Handler for {@link com.polidea.rxandroidble.internal.util.IllegalOperationChecker.MismatchData} returned by
 * {@link IllegalOperationChecker}.
 */
public interface MismatchDataHandler {
    void handleMismatchData(IllegalOperationChecker.MismatchData mismatchData);
}
