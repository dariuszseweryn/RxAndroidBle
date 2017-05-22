package com.polidea.rxandroidble.internal.scan;

import android.bluetooth.BluetoothDevice;
import com.polidea.rxandroidble.scan.ScanCallbackType;
import com.polidea.rxandroidble.scan.ScanRecord;

public class RxBleInternalScanResult {

    private final BluetoothDevice bluetoothDevice;
    private final int rssi;
    private final long timestampNanos;
    private final ScanRecord scanRecord;
    private final ScanCallbackType scanCallbackType;

    public RxBleInternalScanResult(BluetoothDevice bluetoothDevice, int rssi, long timestampNanos, ScanRecord scanRecord,
                                   ScanCallbackType scanCallbackType) {
        this.bluetoothDevice = bluetoothDevice;
        this.rssi = rssi;
        this.timestampNanos = timestampNanos;
        this.scanRecord = scanRecord;
        this.scanCallbackType = scanCallbackType;
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
}
