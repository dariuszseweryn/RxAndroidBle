package com.polidea.rxandroidble.exceptions;

import android.content.Context;

/**
 * Exception emitted when the BLE link has been interrupted as a result of an error. The exception contains
 * detailed explanation of the error source (type of operation) and the code proxied from
 * the Android system.
 *
 * @see com.polidea.rxandroidble.RxBleDevice#establishConnection(Context, boolean)
 */
public class BleGattException extends BleException {

    public static final int UNKNOWN_STATUS = -1;
    private final int status;
    private final BleGattOperationType bleGattOperationType;

    public BleGattException(int status, BleGattOperationType bleGattOperationType) {
        this.status = status;
        this.bleGattOperationType = bleGattOperationType;
    }

    public BleGattException(BleGattOperationType bleGattOperationType) {
        this(UNKNOWN_STATUS, bleGattOperationType);
    }

    public BleGattOperationType getBleGattOperationType() {
        return bleGattOperationType;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' + "status=" + status + ", bleGattOperation=" + bleGattOperationType + '}';
    }
}
