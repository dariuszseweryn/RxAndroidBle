package com.polidea.rxandroidble.exceptions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
        super(createMessage(null, null));
        bluetoothDeviceAddress = "";
    }

    public BleDisconnectedException(Throwable throwable, @NonNull String bluetoothDeviceAddress) {
        super(createMessage(throwable, bluetoothDeviceAddress));
        this.bluetoothDeviceAddress = bluetoothDeviceAddress;
        initCause(throwable);
    }

    public BleDisconnectedException(@NonNull String bluetoothDeviceAddress) {
        super(createMessage(null, bluetoothDeviceAddress));
        this.bluetoothDeviceAddress = bluetoothDeviceAddress;
    }

    private static String createMessage(@Nullable Throwable throwable, @Nullable String bluetoothDeviceAddress) {
        return "BleDisconnectedException{"
                + "bluetoothDeviceAddress='" + bluetoothDeviceAddress + '\''
                + toStringCauseIfExists(throwable)
                + '}';
    }
}
