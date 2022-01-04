package com.polidea.rxandroidble2.internal.util;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;

import java.util.List;

import bleshadow.javax.inject.Inject;

public class BluetoothManagerWrapper {

    private final BluetoothManager bluetoothManager;

    @Inject
    public BluetoothManagerWrapper(BluetoothManager bluetoothManager) {
        this.bluetoothManager = bluetoothManager;
    }

    public List<BluetoothDevice> getConnectedPeripherals() {
        return bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
    }
}
