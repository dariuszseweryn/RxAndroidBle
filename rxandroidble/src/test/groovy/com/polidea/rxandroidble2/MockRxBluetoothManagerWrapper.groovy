package com.polidea.rxandroidble2

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import com.polidea.rxandroidble2.internal.util.RxBluetoothManagerWrapper

class MockBluetoothManagerWrapper extends RxBluetoothManagerWrapper {

    private List<BluetoothDevice> connectedDevices = new ArrayList<>()

    MockBluetoothManagerWrapper() {
        super(null)
    }

    MockBluetoothManagerWrapper(BluetoothManager bluetoothManager) {
        super(bluetoothManager)
    }

    def addConnectedDevice(BluetoothDevice bluetoothDevice) {
        connectedDevices.add(bluetoothDevice)
    }

    @Override
    List<BluetoothDevice> getConnectedDevices() {
        return connectedDevices
    }
}