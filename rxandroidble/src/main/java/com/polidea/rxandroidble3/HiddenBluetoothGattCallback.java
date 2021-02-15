package com.polidea.rxandroidble3;

import android.bluetooth.BluetoothGatt;

/**
 * An interface that represents methods hidden in {@link android.bluetooth.BluetoothGattCallback}
 */
public interface HiddenBluetoothGattCallback {

    void onConnectionUpdated(BluetoothGatt gatt, int interval, int latency, int timeout, int status);
}
