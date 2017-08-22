package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import com.polidea.rxandroidble.internal.scan.RxBleInternalScanResultLegacy;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.internal.util.UUIDUtil;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import rx.Emitter;

public class LegacyScanOperation extends ScanOperation<RxBleInternalScanResultLegacy, BluetoothAdapter.LeScanCallback> {

    private final boolean isFilterDefined;
    private final UUIDUtil uuidUtil;
    private final Set<UUID> filterUuids;

    public LegacyScanOperation(UUID[] filterServiceUUIDs, RxBleAdapterWrapper rxBleAdapterWrapper, final UUIDUtil uuidUtil) {
        super(rxBleAdapterWrapper);

        this.isFilterDefined = filterServiceUUIDs != null && filterServiceUUIDs.length > 0;
        this.uuidUtil = uuidUtil;
        if (this.isFilterDefined) {
            this.filterUuids = new HashSet<>(filterServiceUUIDs.length);
            Collections.addAll(filterUuids, filterServiceUUIDs);
        } else {
            this.filterUuids = null;
        }
    }

    @Override
    BluetoothAdapter.LeScanCallback createScanCallback(final Emitter<RxBleInternalScanResultLegacy> emitter) {
        return new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

                if (!isFilterDefined || uuidUtil.extractUUIDs(scanRecord).containsAll(filterUuids)) {
                    emitter.onNext(new RxBleInternalScanResultLegacy(device, rssi, scanRecord));
                }
            }
        };
    }

    @Override
    boolean startScan(RxBleAdapterWrapper rxBleAdapterWrapper, BluetoothAdapter.LeScanCallback scanCallback) {
        return rxBleAdapterWrapper.startLegacyLeScan(scanCallback);
    }

    @Override
    void stopScan(RxBleAdapterWrapper rxBleAdapterWrapper, BluetoothAdapter.LeScanCallback scanCallback) {
        // TODO: [PU] 29.01.2016 https://code.google.com/p/android/issues/detail?id=160503
        rxBleAdapterWrapper.stopLegacyLeScan(scanCallback);
    }
}
