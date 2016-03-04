package com.polidea.rxandroidble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import com.polidea.rxandroidble.internal.RxBleAdapterWrapper

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

    MockRxBleAdapterWrapper() {
        super(null)
    }

    def addScanResult(BluetoothDevice bluetoothDevice, int rssi, byte[] scanResult) {
        scanDataList.add(new ScanData(bluetoothDevice, rssi, scanResult))
    }

    @Override
    BluetoothDevice getRemoteDevice(String macAddress) {
        scanDataList.find {
            it.device.getAddress() == macAddress
        }?.device
    }

    @Override
    boolean startLeScan(BluetoothAdapter.LeScanCallback callback) {

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
    void stopLeScan(BluetoothAdapter.LeScanCallback leScanCallback) {

    }
}
