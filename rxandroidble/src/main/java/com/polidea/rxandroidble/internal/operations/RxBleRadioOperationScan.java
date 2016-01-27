package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothAdapter;

import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleDeviceImpl;
import com.polidea.rxandroidble.RxBleScanResult;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class RxBleRadioOperationScan extends RxBleRadioOperation<RxBleScanResult> {

    private final UUID[] filterServiceUUIDs;
    private final BluetoothAdapter bluetoothAdapter;

    private final AtomicReference<RxBleRadio> rxBleRadioAtomicReference = new AtomicReference<>();

    private final Map<String, RxBleDevice> availableDevices = new HashMap<>();

    private final BluetoothAdapter.LeScanCallback leScanCallback = (device, rssi, scanRecord) -> {
        final RxBleDevice availableDevice = availableDevices.get(device.getAddress());
        final RxBleDevice returnDevice;
        if (availableDevice == null) {
            returnDevice = new RxBleDeviceImpl(device, rxBleRadioAtomicReference.get());
            availableDevices.put(device.getAddress(), returnDevice);
        } else {
            returnDevice = availableDevice;
        }

        onNext(new RxBleScanResult(returnDevice, rssi, scanRecord));
    };

    public RxBleRadioOperationScan(UUID[] filterServiceUUIDs, BluetoothAdapter bluetoothAdapter, RxBleRadio rxBleRadio) {

        this.filterServiceUUIDs = filterServiceUUIDs;
        this.bluetoothAdapter = bluetoothAdapter;
        this.rxBleRadioAtomicReference.set(rxBleRadio);
    }

    @Override
    public void run() {

        if (filterServiceUUIDs == null) {
            bluetoothAdapter.startLeScan(leScanCallback);
        } else {
            bluetoothAdapter.startLeScan(filterServiceUUIDs, leScanCallback);
        }
    }

    public void stop() {
        bluetoothAdapter.stopLeScan(leScanCallback);
        releaseRadio();
    }
}
