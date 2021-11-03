package com.polidea.rxandroidble2.internal.util;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;

import java.util.List;

import bleshadow.javax.inject.Inject;

public class RxBluetoothManagerWrapper {

    private final BluetoothManager bluetoothManager;

    @Inject
    public RxBluetoothManagerWrapper(BluetoothManager bluetoothManager) {
        this.bluetoothManager = bluetoothManager;
    }

    public List<BluetoothDevice> getConnectedDevices(int profile) {
        return bluetoothManager.getConnectedDevices(profile);
    }
}
