package com.polidea.rxandroidble3

import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.Build
import androidx.annotation.RequiresApi
import com.polidea.rxandroidble3.internal.util.RxBleAdapterWrapper

class MockRxBleAdapterWrapper extends RxBleAdapterWrapper {

    static class ScanData {
        BluetoothDevice device
        int rssi
        byte[] scanRecord

        ScanData(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
            this.device = bluetoothDevice
            this.rssi = rssi
            this.scanRecord = scanRecord
        }
    }

    private List<ScanData> scanDataList = new ArrayList<>()
    private Set<BluetoothDevice> bondedDevices = new HashSet<>()

    MockRxBleAdapterWrapper() {
        super(null)
    }

    MockRxBleAdapterWrapper(BluetoothAdapter bluetoothAdapter) {
        super(bluetoothAdapter)
    }

    def addScanResult(BluetoothDevice bluetoothDevice, int rssi, byte[] scanResult) {
        scanDataList.add(new ScanData(bluetoothDevice, rssi, scanResult))
    }

    def addBondedDevice(BluetoothDevice bluetoothDevice) {
        bondedDevices.add(bluetoothDevice)
    }

    @Override
    BluetoothDevice getRemoteDevice(String macAddress) {
        scanDataList.find {
            it.device.getAddress() == macAddress
        }?.device
    }

    @Override
    boolean startLegacyLeScan(BluetoothAdapter.LeScanCallback callback) {

        scanDataList.each {
            callback.onLeScan(it.device, it.rssi, it.scanRecord)
        }

        return true
    }

    @Override
    boolean hasBluetoothAdapter() {
        return true
    }

    @Override
    boolean isBluetoothEnabled() {
        return true
    }

    @Override
    void stopLegacyLeScan(BluetoothAdapter.LeScanCallback leScanCallback) {

    }

    @RequiresApi(26 /* Build.VERSION_CODES.O */)
    public int startLeScan(List<ScanFilter> scanFilters, ScanSettings scanSettings, PendingIntent callbackIntent) {
        return 0
    }

    @RequiresApi(26 /* Build.VERSION_CODES.O */)
    public void stopLeScan(PendingIntent callbackIntent) {

    }

    @Override
    Set<BluetoothDevice> getBondedDevices() {
        return bondedDevices
    }
}
