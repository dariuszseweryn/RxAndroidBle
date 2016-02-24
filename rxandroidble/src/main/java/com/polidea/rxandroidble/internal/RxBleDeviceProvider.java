package com.polidea.rxandroidble.internal;

import android.bluetooth.BluetoothAdapter;

import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleDeviceImpl;

import java.util.Map;

public class RxBleDeviceProvider {

    private final Map<String, RxBleDevice> availableDevices = new RxBleDeviceCache();
    private final BluetoothAdapter bluetoothAdapter;
    private final RxBleRadio rxBleRadio;

    public RxBleDeviceProvider(BluetoothAdapter bluetoothAdapter, RxBleRadio rxBleRadio) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.rxBleRadio = rxBleRadio;
    }

    public RxBleDevice getBleDevice(String macAddress) {
        final RxBleDevice rxBleDevice = availableDevices.get(macAddress);

        if (rxBleDevice != null) {
            return rxBleDevice;
        }

        synchronized (availableDevices) {
            final RxBleDevice secondCheckRxBleDevice = availableDevices.get(macAddress);

            if (secondCheckRxBleDevice != null) {
                return secondCheckRxBleDevice;
            }

            final RxBleDeviceImpl newRxBleDevice = new RxBleDeviceImpl(bluetoothAdapter.getRemoteDevice(macAddress), rxBleRadio);
            availableDevices.put(macAddress, newRxBleDevice);
            return newRxBleDevice;
        }
    }
}
