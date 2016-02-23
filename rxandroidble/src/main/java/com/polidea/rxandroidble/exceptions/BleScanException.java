package com.polidea.rxandroidble.exceptions;

public class BleScanException extends BleException {

    public static final int BLE_CANNOT_START = 0;
    public static final int BLUETOOTH_DISABLED = 1;

    private final int cause;

    public BleScanException(int cause) {
        this.cause = cause;
    }
}
