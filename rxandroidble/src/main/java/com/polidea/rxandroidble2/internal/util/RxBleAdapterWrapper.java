package com.polidea.rxandroidble2.internal.util;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.polidea.rxandroidble2.internal.RxBleLog;
import java.util.List;
import java.util.Set;

import bleshadow.javax.inject.Inject;

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

    @TargetApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
    public void startLeScan(List<ScanFilter> scanFilters, ScanSettings scanSettings, ScanCallback scanCallback) {
        bluetoothAdapter.getBluetoothLeScanner().startScan(scanFilters, scanSettings, scanCallback);
    }

    @RequiresApi(26 /* Build.VERSION_CODES.O */)
    public int startLeScan(List<ScanFilter> scanFilters, ScanSettings scanSettings, PendingIntent callbackIntent) {
        return bluetoothAdapter.getBluetoothLeScanner().startScan(scanFilters, scanSettings, callbackIntent);
    }

    @RequiresApi(26 /* Build.VERSION_CODES.O */)
    public void stopLeScan(PendingIntent callbackIntent) {
        bluetoothAdapter.getBluetoothLeScanner().stopScan(callbackIntent);
    }

    @TargetApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
    public void stopLeScan(ScanCallback scanCallback) {
        final BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            RxBleLog.v(
                    "Cannot call BluetoothLeScanner.stopScan(ScanCallback) on 'null' reference because BluetoothAdapter.isEnabled() == %b",
                    bluetoothAdapter.isEnabled()
            );
            // if stopping the scan is not possible due to BluetoothLeScanner not accessible then it is probably stopped anyway
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            // this situation seems to be a problem since API 29
            RxBleLog.v(
                    "BluetoothAdapter is disabled, calling BluetoothLeScanner.stopScan(ScanCallback) may cause IllegalStateException"
            );
            // if stopping the scan is not possible due to BluetoothAdapter turned off then it is probably stopped anyway
            return;
        }
        bluetoothLeScanner.stopScan(scanCallback);
    }

    public Set<BluetoothDevice> getBondedDevices() {
        return bluetoothAdapter.getBondedDevices();
    }
}
