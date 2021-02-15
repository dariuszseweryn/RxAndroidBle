package com.polidea.rxandroidble2.internal.scan;

import android.bluetooth.BluetoothDevice;

public class RxBleInternalScanResultLegacy {

    private final BluetoothDevice bluetoothDevice;
    private final int rssi;
    private final byte[] scanRecord;

    public RxBleInternalScanResultLegacy(BluetoothDevice bleDevice, int rssi, byte[] scanRecords) {
        this.bluetoothDevice = bleDevice;
        this.rssi = rssi;
        this.scanRecord = scanRecords;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public int getRssi() {
        return rssi;
    }

    public byte[] getScanRecord() {
        return scanRecord;
    }
}
