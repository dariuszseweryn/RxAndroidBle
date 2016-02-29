package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothAdapter;

import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.UUIDUtil;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class RxBleRadioOperationScan extends RxBleRadioOperation<RxBleInternalScanResult> {

    private final UUID[] filterServiceUUIDs;
    private final BluetoothAdapter bluetoothAdapter;
    private final UUIDUtil uuidUtil;
    private final AtomicBoolean isScanning = new AtomicBoolean(false);

    private final BluetoothAdapter.LeScanCallback leScanCallback = (device, rssi, scanRecord) -> {

        if (!hasDefinedFilter() || hasDefinedFilter() && containsDesiredServiceIds(scanRecord)) {
            onNext(new RxBleInternalScanResult(device, rssi, scanRecord));
        }
    };

    public RxBleRadioOperationScan(UUID[] filterServiceUUIDs, BluetoothAdapter bluetoothAdapter, UUIDUtil uuidUtil) {

        this.filterServiceUUIDs = filterServiceUUIDs;
        this.bluetoothAdapter = bluetoothAdapter;
        this.uuidUtil = uuidUtil;
    }

    @Override
    public void run() {

        try {
            isScanning.set(true);
            boolean startLeScanStatus = bluetoothAdapter.startLeScan(leScanCallback);

            if (!startLeScanStatus) {
                onError(new BleScanException(BleScanException.BLE_CANNOT_START));
            }
        } finally {
            releaseRadio();
        }
    }

    public void stop() {

        if (isScanning.compareAndSet(true, false)) {
            // TODO: [PU] 29.01.2016 https://code.google.com/p/android/issues/detail?id=160503
            bluetoothAdapter.stopLeScan(leScanCallback);
            isScanning.set(false);
        }
    }

    private boolean containsDesiredServiceIds(byte[] scanRecord) {
        List<UUID> advertisedUUIDs = uuidUtil.extractUUIDs(scanRecord);

        for (UUID desiredUUID : filterServiceUUIDs) {

            if (advertisedUUIDs.contains(desiredUUID)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasDefinedFilter() {
        return filterServiceUUIDs != null && filterServiceUUIDs.length > 0;
    }
}
