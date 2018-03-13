package com.polidea.rxandroidble2.exceptions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Exception emitted when the BLE link has been disconnected either when the connection was already established
 * or was in pending connection state. This state is expected when the connection was released as a
 * part of expected behavior (with {@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} state).
 *
 * @see com.polidea.rxandroidble2.RxBleDevice#establishConnection(boolean)
 */
public class BleDisconnectedException extends BleException {

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public final String bluetoothDeviceAddress;

    @Deprecated
    public BleDisconnectedException() {
        bluetoothDeviceAddress = "";
    }

    public BleDisconnectedException(Throwable throwable, @NonNull String bluetoothDeviceAddress) {
        super(createMessage(bluetoothDeviceAddress), throwable);
        this.bluetoothDeviceAddress = bluetoothDeviceAddress;
    }

    public BleDisconnectedException(@NonNull String bluetoothDeviceAddress) {
        super(createMessage(bluetoothDeviceAddress));
        this.bluetoothDeviceAddress = bluetoothDeviceAddress;
    }

    private static String createMessage(@Nullable String bluetoothDeviceAddress) {
        return "Disconnected from " + bluetoothDeviceAddress;
    }
}
