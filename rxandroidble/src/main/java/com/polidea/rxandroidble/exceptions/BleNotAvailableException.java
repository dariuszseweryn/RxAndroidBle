package com.polidea.rxandroidble.exceptions;

public class BleNotAvailableException extends BleException {

    public static final int NOT_AVAILABLE = 0;

    public static final int NOT_ENABLED = 1;

    private final int cause;

    public BleNotAvailableException(int cause) {

        this.cause = cause;
    }
}
