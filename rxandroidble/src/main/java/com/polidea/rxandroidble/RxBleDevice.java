package com.polidea.rxandroidble;

import android.bluetooth.BluetoothGattCallback;
import android.content.Context;

import rx.Observable;

public interface RxBleDevice {

    /**
     * This observable returns only actual state of the BLE connection - it doesn't transmit errors.
     * On subscription it returns immediately last known RxBleConnectionState.
     *
     * @return the most current RxBleConnectionState
     */
    Observable<RxBleConnection.RxBleConnectionState> getConnectionState();

    /**
     * Establishes connection with a given BLE device. {@link RxBleConnection} is a handle, used to process BLE operations with connected
     * device.
     *
     * @param context     Android's context.
     * @param autoConnect Marker related with
     *                    {@link android.bluetooth.BluetoothDevice#connectGatt(Context, boolean, BluetoothGattCallback)} autoConnect flag.
     *                    In case of auto connect is enabled the observable will wait with the emission of RxBleConnection. Without
     *                    auto connect the connection will fail if the device is not in range.
     * @return Observable emitting the connection.
     * @throws
     */
    Observable<RxBleConnection> establishConnection(Context context, boolean autoConnect);

    /**
     * Name of the device. Name is optional and it's up to the device vendor if will be provided.
     *
     * @return The device name or null if device name is absent.
     */
    String getName();

    /**
     * MAC address of the corresponding device.
     */
    String getMacAddress();
}
