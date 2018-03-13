package com.polidea.rxandroidble2.exceptions;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;

public class BleGattDescriptorException extends BleGattException {

    public final BluetoothGattDescriptor descriptor;

    public BleGattDescriptorException(
            BluetoothGatt gatt,
            BluetoothGattDescriptor descriptor,
            int status,
            BleGattOperationType bleGattOperationType
    ) {
        super(gatt, status, bleGattOperationType);
        this.descriptor = descriptor;
    }
}
