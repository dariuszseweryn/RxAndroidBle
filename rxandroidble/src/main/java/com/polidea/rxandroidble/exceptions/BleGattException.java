package com.polidea.rxandroidble.exceptions;

import com.polidea.rxandroidble.exceptions.BleGattOperationType;

public class BleGattException extends BleException {

    private final int status;

    private final BleGattOperationType bleGattOperationType;

    public BleGattException(int status, BleGattOperationType bleGattOperationType) {
        this.status = status;
        this.bleGattOperationType = bleGattOperationType;
    }

    public int getStatus() {
        return status;
    }

    public BleGattOperationType getBleGattOperationType() {
        return bleGattOperationType;
    }

    @Override
    public String toString() {
        return "BleGattException{" +
                "status=" + status +
                ", bleGattOperation=" + bleGattOperationType +
                '}';
    }
}
