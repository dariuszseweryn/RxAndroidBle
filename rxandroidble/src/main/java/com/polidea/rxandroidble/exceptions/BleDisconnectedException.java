package com.polidea.rxandroidble.exceptions;

import android.support.annotation.NonNull;

/**
 * Exception emitted when the BLE link has been disconnected either when the connection was already established
 * or was in pending connection state. This state is expected when the connection was released as a
 * part of expected behavior (with {@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} state).
 *
 * @see com.polidea.rxandroidble.RxBleDevice#establishConnection(Context, boolean)
 */
public class BleDisconnectedException extends BleException {

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public final String bluetoothDeviceAddress;

    @Deprecated
    public BleDisconnectedException() {
        super();
        bluetoothDeviceAddress = "";
    }

    public BleDisconnectedException(Throwable throwable, @NonNull String bluetoothDeviceAddress) {
        super(throwable);
        this.bluetoothDeviceAddress = bluetoothDeviceAddress;
    }

    public BleDisconnectedException(@NonNull String bluetoothDeviceAddress) {
        this.bluetoothDeviceAddress = bluetoothDeviceAddress;
    }

    @Override
    public String toString() {
        return "BleDisconnectedException{"
                + "bluetoothDeviceAddress='" + bluetoothDeviceAddress + '\''
                + toStringCauseIfExists()
                + '}';
    }
}
