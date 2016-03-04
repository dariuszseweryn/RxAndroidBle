package com.polidea.rxandroidble.exceptions;

public class BleScanException extends BleException {

    public static final int BLE_CANNOT_START = 0;
    public static final int BLUETOOTH_DISABLED = 1;
    public static final int BLUETOOTH_NOT_AVAILABLE = 2;

    private final int reason;

    public BleScanException(int cause) {
        this.reason = cause;
    }

    public int getReason() {
        return reason;
    }
}
