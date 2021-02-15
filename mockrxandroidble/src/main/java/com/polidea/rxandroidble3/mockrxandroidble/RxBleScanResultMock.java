package com.polidea.rxandroidble3.mockrxandroidble;

import com.polidea.rxandroidble3.RxBleDevice;
import com.polidea.rxandroidble3.internal.ScanResultInterface;
import com.polidea.rxandroidble3.scan.ScanCallbackType;
import com.polidea.rxandroidble3.scan.ScanRecord;
import com.polidea.rxandroidble3.scan.ScanResult;

public class RxBleScanResultMock extends ScanResult implements ScanResultInterface {
    public RxBleScanResultMock(RxBleDevice bleDevice, int rssi, long timestampNanos, ScanCallbackType callbackType, ScanRecord scanRecord) {
        super(bleDevice, rssi, timestampNanos, callbackType, scanRecord);
    }

    public String getAddress() {
        RxBleDevice device = getBleDevice();
        return device == null ? null : device.getMacAddress();
    }

    public String getDeviceName() {
        RxBleDevice device = getBleDevice();
        return device == null ? null : device.getName();
    }

    public ScanCallbackType getScanCallbackType() {
        return getCallbackType();
    }
}
