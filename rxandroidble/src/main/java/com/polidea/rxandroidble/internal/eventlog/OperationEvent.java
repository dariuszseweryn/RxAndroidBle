package com.polidea.rxandroidble.internal.eventlog;

import com.polidea.rxandroidble.internal.operations.Operation;
import com.polidea.rxandroidble.utils.BytePrinter;

public class OperationEvent {

    public final int operationId;
    public final String title;
    public final String operationName;
    public final OperationDescription attributes;
    public final byte[] payload;

    public static int operationIdentifierHash(Operation operation) {
        return System.identityHashCode(operation);
    }

    public OperationEvent(int operationId, String title, String operationName, OperationDescription attributes, byte[] payload) {
        this.operationId = operationId;
        this.title = title;
        this.operationName = operationName;
        this.attributes = attributes;
        this.payload = payload;
    }

    public OperationEvent(int operationId, String title, String operationName, OperationDescription attributes) {
        this(operationId, title, operationName, attributes, null);
    }

    public OperationEvent(int operationId, String title, String operationName) {
        this(operationId, title, operationName, new OperationDescription());
    }

    @Override
    public String toString() {
        return "OperationEvent{"
                + "operationId=" + operationId
                + ", title='" + title + '\''
                + ", operationName='" + operationName + '\''
                + ", attributes=" + attributes
                + ", payload=" + BytePrinter.toPrettyFormattedHexString(payload)
                + '}';
    }
}