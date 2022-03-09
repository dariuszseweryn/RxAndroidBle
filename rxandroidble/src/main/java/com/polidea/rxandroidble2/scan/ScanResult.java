package com.polidea.rxandroidble2.scan;

import androidx.annotation.NonNull;

import com.polidea.rxandroidble2.IsConnectableStatus;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.internal.logger.LoggerUtil;

public class ScanResult {

    private final RxBleDevice bleDevice;
    private final int rssi;
    private final long timestampNanos;
    private final ScanCallbackType callbackType;
    private final ScanRecord scanRecord;
    private final IsConnectableStatus isConnectable;

    public ScanResult(RxBleDevice bleDevice, int rssi, long timestampNanos, ScanCallbackType callbackType,
                      ScanRecord scanRecord, IsConnectableStatus isConnectable) {
        this.bleDevice = bleDevice;
        this.rssi = rssi;
        this.timestampNanos = timestampNanos;
        this.callbackType = callbackType;
        this.scanRecord = scanRecord;
        this.isConnectable = isConnectable;
    }

    public RxBleDevice getBleDevice() {
        return bleDevice;
    }

    public int getRssi() {
        return rssi;
    }

    public long getTimestampNanos() {
        return timestampNanos;
    }

    public ScanCallbackType getCallbackType() {
        return callbackType;
    }

    public ScanRecord getScanRecord() {
        return scanRecord;
    }

    public IsConnectableStatus isConnectable() {
        return isConnectable;
    }

    @Override
    @NonNull
    public String toString() {
        return "ScanResult{"
                + "bleDevice=" + bleDevice
                + ", rssi=" + rssi
                + ", timestampNanos=" + timestampNanos
                + ", callbackType=" + callbackType
                + ", scanRecord=" + LoggerUtil.bytesToHex(scanRecord.getBytes())
                + ", isconnectable=" + isConnectable
                + '}';
    }
}
