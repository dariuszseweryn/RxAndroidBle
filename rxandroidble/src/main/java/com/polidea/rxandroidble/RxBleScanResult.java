package com.polidea.rxandroidble;

public class RxBleScanResult {

    private final RxBleDevice bleDevice;

    private final int rssi;

    private final byte[] scanRecords;

    public RxBleScanResult(RxBleDevice bleDevice, int rssi, byte[] scanRecords) {
        this.bleDevice = bleDevice;
        this.rssi = rssi;
        this.scanRecords = scanRecords;
    }

    public RxBleDevice getBleDevice() {
        return bleDevice;
    }

    public int getRssi() {
        return rssi;
    }

    public byte[] getScanRecords() {
        return scanRecords;
    }
}
