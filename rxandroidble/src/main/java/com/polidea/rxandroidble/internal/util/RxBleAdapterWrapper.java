package com.polidea.rxandroidble.internal.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.support.annotation.Nullable;

import java.util.Set;

import javax.inject.Inject;

public class RxBleAdapterWrapper {

    private final BluetoothAdapter bluetoothAdapter;

    @Inject
    public RxBleAdapterWrapper(@Nullable BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter;
    }

    public BluetoothDevice getRemoteDevice(String macAddress) {
        return bluetoothAdapter.getRemoteDevice(macAddress);
    }

    public boolean hasBluetoothAdapter() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public boolean startLeScan(BluetoothAdapter.LeScanCallback leScanCallback) {
        return bluetoothAdapter.startLeScan(leScanCallback);
    }

    public void stopLeScan(BluetoothAdapter.LeScanCallback leScanCallback) {
        bluetoothAdapter.stopLeScan(leScanCallback);
    }

    public Set<BluetoothDevice> getBondedDevices() {
        return bluetoothAdapter.getBondedDevices();
    }
}
