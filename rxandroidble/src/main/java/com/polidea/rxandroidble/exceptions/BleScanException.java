package com.polidea.rxandroidble.exceptions;

public class BleScanException extends BleException {

    public static final int BLE_CANNOT_START = 0;

    private final int cause;

    public BleScanException(int cause) {
        this.cause = cause;
    }
}
