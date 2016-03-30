package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothAdapter;

import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.internal.util.UUIDUtil;

import java.util.List;
import java.util.UUID;

public class RxBleRadioOperationScan extends RxBleRadioOperation<RxBleInternalScanResult> {

    private final UUID[] filterServiceUUIDs;
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final UUIDUtil uuidUtil;

    private final BluetoothAdapter.LeScanCallback leScanCallback = (device, rssi, scanRecord) -> {

        if (!hasDefinedFilter() || hasDefinedFilter() && containsDesiredServiceIds(scanRecord)) {
            onNext(new RxBleInternalScanResult(device, rssi, scanRecord));
        }
    };

    public RxBleRadioOperationScan(UUID[] filterServiceUUIDs, RxBleAdapterWrapper rxBleAdapterWrapper, UUIDUtil uuidUtil) {

        this.filterServiceUUIDs = filterServiceUUIDs;
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.uuidUtil = uuidUtil;
    }

    @Override
    public void run() {

        try {
            boolean startLeScanStatus = rxBleAdapterWrapper.startLeScan(leScanCallback);

            if (!startLeScanStatus) {
                onError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START));
            }
        } finally {
            releaseRadio();
        }
    }

    public void stop() {
        // TODO: [PU] 29.01.2016 https://code.google.com/p/android/issues/detail?id=160503
        rxBleAdapterWrapper.stopLeScan(leScanCallback);
    }

    private boolean containsDesiredServiceIds(byte[] scanRecord) {
        List<UUID> advertisedUUIDs = uuidUtil.extractUUIDs(scanRecord);

        for (UUID desiredUUID : filterServiceUUIDs) {

            if (!advertisedUUIDs.contains(desiredUUID)) {
                return false;
            }
        }

        return true;
    }

    private boolean hasDefinedFilter() {
        return filterServiceUUIDs != null && filterServiceUUIDs.length > 0;
    }
}
