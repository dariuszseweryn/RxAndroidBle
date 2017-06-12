package com.polidea.rxandroidble.internal.util;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.internal.RxBleLog;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class RxBleAdapterWrapper {

    private final BluetoothAdapter bluetoothAdapter;

    @Inject
    public RxBleAdapterWrapper(@Nullable BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter;
    }

    public BluetoothDevice getRemoteDevice(String macAddress) {
        return bluetoothAdapter.getRemoteDevice(macAddress);
    }

    public boolean hasBluetoothAdapter() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public boolean startLegacyLeScan(BluetoothAdapter.LeScanCallback leScanCallback) {
        return bluetoothAdapter.startLeScan(leScanCallback);
    }

    public void stopLegacyLeScan(BluetoothAdapter.LeScanCallback leScanCallback) {
        bluetoothAdapter.stopLeScan(leScanCallback);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void startLeScan(List<ScanFilter> scanFilters, ScanSettings scanSettings, ScanCallback scanCallback) {
        bluetoothAdapter.getBluetoothLeScanner().startScan(scanFilters, scanSettings, scanCallback);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void stopLeScan(ScanCallback scanCallback) {
        final BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            RxBleLog.d("Cannot perform BluetoothLeScanner.stopScan(ScanCallback) because scanner is unavailable (Probably adapter is off)");
            // if stopping the scan is not possible due to BluetoothLeScanner not accessible then it is probably stopped anyway
            return;
        }
        bluetoothLeScanner.stopScan(scanCallback);
    }

    public Set<BluetoothDevice> getBondedDevices() {
        return bluetoothAdapter.getBondedDevices();
    }
}
