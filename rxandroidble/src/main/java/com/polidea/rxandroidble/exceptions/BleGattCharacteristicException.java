package com.polidea.rxandroidble.exceptions;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

public class BleGattCharacteristicException extends BleGattException {

    public final BluetoothGattCharacteristic characteristic;

    public BleGattCharacteristicException(
            BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic,
            int status,
            BleGattOperationType bleGattOperationType
    ) {
        super(gatt, status, bleGattOperationType);
        this.characteristic = characteristic;
    }
}
