package com.polidea.rxandroidble2.exceptions;


import android.bluetooth.BluetoothGatt;

/**
 * This exception is used when a call on a {@link BluetoothGatt} has returned true (succeeded) but the corresponding
 * {@link android.bluetooth.BluetoothGattCallback} callback was not called after a certain time (usually 30 seconds)
 * which is considered a Android OS BLE Stack misbehaviour
 */
public class BleGattCallbackTimeoutException extends BleGattException {

    public BleGattCallbackTimeoutException(BluetoothGatt gatt, BleGattOperationType bleGattOperationType) {
        super(gatt, bleGattOperationType);
    }
}
