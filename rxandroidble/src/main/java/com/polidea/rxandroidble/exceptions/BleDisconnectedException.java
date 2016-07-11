package com.polidea.rxandroidble.exceptions;

/**
 * Exception emitted when the BLE link has been disconnected either when the connection was already established
 * or was in pending connection state. This state is expected when the connection was released as a
 * part of expected behavior (with {@link android.bluetooth.BluetoothGatt#GATT_SUCCESS} state).
 *
 * @see com.polidea.rxandroidble.RxBleDevice#establishConnection(Context, boolean)
 */
public class BleDisconnectedException extends BleException {

    public BleDisconnectedException() {
        super();
    }

    public BleDisconnectedException(Throwable throwable) {
        super(throwable);
    }
}
