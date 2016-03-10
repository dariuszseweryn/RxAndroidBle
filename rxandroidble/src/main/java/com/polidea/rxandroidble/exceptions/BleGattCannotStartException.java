package com.polidea.rxandroidble.exceptions;

public class BleGattCannotStartException extends BleGattException {

    public BleGattCannotStartException(BleGattOperationType bleGattOperationType) {
        super(UNKNOWN_STATUS, bleGattOperationType);
    }
}
