package com.polidea.rxandroidble.exceptions;

import android.bluetooth.BluetoothGattCharacteristic;

public class BleCannotSetCharacteristicNotificationException extends BleException {

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;

    public BleCannotSetCharacteristicNotificationException(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
    }

    public BluetoothGattCharacteristic getBluetoothGattCharacteristic() {
        return bluetoothGattCharacteristic;
    }

    @Override
    public String toString() {
        return "BleCannotSetCharacteristicNotificationException{" +
                "bluetoothGattCharacteristic=" + bluetoothGattCharacteristic.getUuid() +
                '}';
    }
}
