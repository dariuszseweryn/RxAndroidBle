package com.polidea.rxandroidble.eventlog;

import android.support.annotation.Nullable;

public interface OperationEventLogger {

    void onOperationEnqueued(OperationEvent event);
    void onOperationStarted(OperationEvent event);
    void onOperationFinished(OperationEvent event);
    void onOperationFinished(OperationEvent event,  @Nullable String result);
    void onOperationFailed(OperationEvent event, String message);
    void onAtomicOperation(OperationEvent event, @Nullable String result);
    void onAtomicOperation(OperationEvent event);
    boolean isAttached();
}