package com.polidea.rxandroidble.internal;

import android.bluetooth.BluetoothDevice;

import com.polidea.rxandroidble.RxBleScanRecord;

public class RxBleInternalScanResultV21 {

    private final BluetoothDevice bluetoothDevice;
    private final int rssi;
    private final RxBleScanRecord scanRecord;

    public RxBleInternalScanResultV21(BluetoothDevice bleDevice, int rssi, RxBleScanRecord scanRecords) {
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

    public RxBleScanRecord getScanRecord() {
        return scanRecord;
    }
}
