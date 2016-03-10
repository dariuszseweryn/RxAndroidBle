package com.polidea.rxandroidble.exceptions;

public class BleGattException extends BleException {

    public static final int UNKNOWN_STATUS = -1;
    public static final int CHARACTERISTIC_NOT_FOUND = 1;
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
        return getClass().getSimpleName() + '{' +
                "status=" + status +
                ", bleGattOperation=" + bleGattOperationType +
                '}';
    }
}
