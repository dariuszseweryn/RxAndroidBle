package com.polidea.rxandroidble.internal.eventlog;

import android.support.annotation.Nullable;

/**
 * Default logger implementation that does not do logging at all. This implementation is used if no other logger is set.
 */
public class HollowEventLogger implements OperationEventLogger {

    @Override
    public void onOperationEnqueued(OperationEvent event) {
        // Nothing logged
    }

    @Override
    public void onOperationStarted(OperationEvent event) {
        // Nothing logged
    }

    @Override
    public void onOperationFinished(OperationEvent event) {
        // Nothing logged
    }

    @Override
    public void onOperationFinished(OperationEvent event, @Nullable String result) {
        // Nothing logged
    }

    @Override
    public void onOperationFailed(OperationEvent event, String message) {
        // Nothing logged
    }

    @Override
    public void onAtomicOperation(OperationEvent event, @Nullable String result) {
        // Nothing logged
    }

    @Override
    public void onAtomicOperation(OperationEvent event) {
        // Nothing logged
    }

    @Override
    public boolean isAttached() {
        return false;
    }
}
