package com.polidea.rxandroidble.exceptions;

import android.bluetooth.BluetoothGatt;

public class BleGattCannotStartException extends BleGattException {

    @Deprecated
    public BleGattCannotStartException(BleGattOperationType bleGattOperationType) {
        super(null, bleGattOperationType);
    }

    public BleGattCannotStartException(BluetoothGatt gatt, BleGattOperationType bleGattOperationType) {
        super(gatt, bleGattOperationType);
    }
}
