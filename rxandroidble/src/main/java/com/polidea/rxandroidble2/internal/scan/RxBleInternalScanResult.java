package com.polidea.rxandroidble2.internal.scan;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

import com.polidea.rxandroidble2.scan.ScanCallbackType;
import com.polidea.rxandroidble2.scan.ScanRecord;

public class RxBleInternalScanResult {

    private final BluetoothDevice bluetoothDevice;
    private final int rssi;
    private final long timestampNanos;
    private final ScanRecord scanRecord;
    private final ScanCallbackType scanCallbackType;
    private final ScanResult scanResult;

    public RxBleInternalScanResult(BluetoothDevice bluetoothDevice, int rssi, long timestampNanos, ScanRecord scanRecord,
                                   ScanCallbackType scanCallbackType, ScanResult scanResult) {
        this.bluetoothDevice = bluetoothDevice;
        this.rssi = rssi;
        this.timestampNanos = timestampNanos;
        this.scanRecord = scanRecord;
        this.scanCallbackType = scanCallbackType;
        this.scanResult = scanResult;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public int getRssi() {
        return rssi;
    }

    public ScanRecord getScanRecord() {
        return scanRecord;
    }

    public long getTimestampNanos() {
        return timestampNanos;
    }

    public ScanCallbackType getScanCallbackType() {
        return scanCallbackType;
    }

    public ScanResult getScanResult() {
        return scanResult;
    }
}
