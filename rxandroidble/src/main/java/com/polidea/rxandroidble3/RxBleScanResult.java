package com.polidea.rxandroidble2;

import com.polidea.rxandroidble2.internal.logger.LoggerUtil;

/**
 * Represents a scan result from Bluetooth LE scan.
 */
public class RxBleScanResult {

    private final RxBleDevice bleDevice;
    private final int rssi;
    private final byte[] scanRecord;

    public RxBleScanResult(RxBleDevice bleDevice, int rssi, byte[] scanRecords) {
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
     * @return Array of data containing full ADV packet.
     */
    public byte[] getScanRecord() {
        return scanRecord;
    }

    @Override
    public String toString() {
        return "RxBleScanResult{"
                + "bleDevice=" + bleDevice
                + ", rssi=" + rssi
                + ", scanRecord=" + LoggerUtil.bytesToHex(scanRecord)
                + '}';
    }
}
