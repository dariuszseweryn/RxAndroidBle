package com.polidea.rxandroidble;

/**
 * Represents a scan result from Bluetooth LE scan.
 */
public class RxBleScanResult {

    private final RxBleDevice bleDevice;
    private final int rssi;
    private final RxBleScanRecord scanRecord;

    public RxBleScanResult(RxBleDevice bleDevice, int rssi, RxBleScanRecord scanRecords) {
        this.bleDevice = bleDevice;
        this.rssi = rssi;
        this.scanRecord = scanRecords;
    }

    /**
     * Returns {@link RxBleDevice} which is a handle for Bluetooth operations on a device. It may be used to establish connection,
     * get MAC address and/or get the device name.
     */
    public RxBleDevice getBleDevice() {
        return bleDevice;
    }

    /**
     * Returns signal strength indication received during scan operation.
     *
     * @return the rssi value
     */
    public int getRssi() {
        return rssi;
    }

    /**
     * The scan record of Bluetooth LE advertisement.
     *
     * @return Scan record, including advertising data and scan response data.
     */
    public RxBleScanRecord getScanRecord() {
        return scanRecord;
    }
}
