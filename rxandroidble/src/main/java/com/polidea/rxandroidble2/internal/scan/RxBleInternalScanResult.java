package com.polidea.rxandroidble2.internal.scan;

import android.bluetooth.BluetoothDevice;

import com.polidea.rxandroidble2.internal.ScanResultInterface;
import com.polidea.rxandroidble2.scan.ScanCallbackType;
import com.polidea.rxandroidble2.scan.ScanRecord;

public class RxBleInternalScanResult implements ScanResultInterface {

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

    @Override
    public int getRssi() {
        return rssi;
    }

    @Override
    public ScanRecord getScanRecord() {
        return scanRecord;
    }

    @Override
    public long getTimestampNanos() {
        return timestampNanos;
    }

    @Override
    public ScanCallbackType getScanCallbackType() {
        return scanCallbackType;
    }

    @Override
    public String getAddress() {
        return bluetoothDevice.getAddress();
    }

    @Override
    public String getDeviceName() {
        BluetoothDevice device = getBluetoothDevice();
        return device == null ? null : device.getName();
    }

}
