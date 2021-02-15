package com.polidea.rxandroidble2.exceptions;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * An exception being emitted from {@link com.polidea.rxandroidble2.RxBleConnection#readCharacteristic(BluetoothGattCharacteristic)}
 * or other characteristic related observables when the {@link android.bluetooth.BluetoothGattCallback} is called with status other than
 * {@link android.bluetooth.BluetoothGatt#GATT_SUCCESS}
 */
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
