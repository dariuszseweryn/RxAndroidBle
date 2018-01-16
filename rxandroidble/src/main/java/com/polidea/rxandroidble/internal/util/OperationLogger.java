package com.polidea.rxandroidble.internal.util;

import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.operations.Operation;

public class OperationLogger {

    public static void logOperationStarted(Operation operation) {
        if (RxBleLog.isAtLeast(RxBleLog.DEBUG)) {
            RxBleLog.d("STARTED %s(%d)", operation.getClass().getSimpleName(), System.identityHashCode(operation));
        }
    }

    public static void logOperationRemoved(Operation operation) {
        if (RxBleLog.isAtLeast(RxBleLog.DEBUG)) {
            RxBleLog.d("REMOVED %s(%d)", operation.getClass().getSimpleName(), System.identityHashCode(operation));
        }
    }

    public static void logOperationQueued(Operation operation) {
        if (RxBleLog.isAtLeast(RxBleLog.DEBUG)) {
            RxBleLog.d("QUEUED %s(%d)", operation.getClass().getSimpleName(), System.identityHashCode(operation));
        }
    }

    public static void logOperationFinished(Operation operation, long startTime, long endTime) {
        if (RxBleLog.isAtLeast(RxBleLog.DEBUG)) {
            RxBleLog.d("FINISHED in %d %s(%d)", (endTime - startTime), operation.getClass().getSimpleName(), System.identityHashCode(operation));
        }
    }
}
