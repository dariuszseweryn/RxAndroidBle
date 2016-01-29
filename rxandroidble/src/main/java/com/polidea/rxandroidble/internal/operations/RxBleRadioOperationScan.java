package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleDeviceImpl;
import com.polidea.rxandroidble.RxBleScanResult;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.UUIDParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class RxBleRadioOperationScan extends RxBleRadioOperation<RxBleScanResult> {

    private final UUID[] filterServiceUUIDs;
    private final BluetoothAdapter bluetoothAdapter;
    private final UUIDParser uuidParser;
    private final AtomicReference<RxBleRadio> rxBleRadioAtomicReference = new AtomicReference<>();
    private final Map<String, RxBleDevice> availableDevices = new HashMap<>();

    private final BluetoothAdapter.LeScanCallback leScanCallback = (device, rssi, scanRecord) -> {

        if (!hasDefinedFilter() || hasDefinedFilter() && containsDesiredServiceIds(scanRecord)) {
            onNext(new RxBleScanResult(createOrRetrieveDevice(device), rssi, scanRecord));
        }
    };

    public RxBleRadioOperationScan(UUID[] filterServiceUUIDs, BluetoothAdapter bluetoothAdapter,
                                   RxBleRadio rxBleRadio, UUIDParser uuidParser) {

        this.filterServiceUUIDs = filterServiceUUIDs;
        this.bluetoothAdapter = bluetoothAdapter;
        this.uuidParser = uuidParser;
        this.rxBleRadioAtomicReference.set(rxBleRadio);
    }

    @Override
    public void run() {
        boolean startLeScanStatus = bluetoothAdapter.startLeScan(leScanCallback);

        if(!startLeScanStatus) {
            onError(new BleScanException(BleScanException.BLE_CANNOT_START));
        }
    }

    public void stop() {
        // TODO: [PU] 29.01.2016 https://code.google.com/p/android/issues/detail?id=160503 
        bluetoothAdapter.stopLeScan(leScanCallback);
        releaseRadio();
    }

    private boolean containsDesiredServiceIds(byte[] scanRecord) {
        List<UUID> advertisedUUIDs = uuidParser.extractUUIDs(scanRecord);

        for (UUID desiredUUID : filterServiceUUIDs) {

            if (advertisedUUIDs.contains(desiredUUID)) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    private RxBleDevice createOrRetrieveDevice(BluetoothDevice device) {
        final RxBleDevice availableDevice = availableDevices.get(device.getAddress());

        if (availableDevice == null) {
            final RxBleDevice returnDevice = new RxBleDeviceImpl(device, rxBleRadioAtomicReference.get());
            availableDevices.put(device.getAddress(), returnDevice);
            return returnDevice;
        } else {
            return availableDevice;
        }
    }

    private boolean hasDefinedFilter() {
        return filterServiceUUIDs != null && filterServiceUUIDs.length > 0;
    }
}
