package com.polidea.rxandroidble2.internal.operations;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.Nullable;
import com.polidea.rxandroidble2.internal.logger.LoggerUtil;
import com.polidea.rxandroidble2.internal.scan.RxBleInternalScanResultLegacy;
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble2.internal.util.UUIDUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import io.reactivex.Emitter;

public class LegacyScanOperation extends ScanOperation<RxBleInternalScanResultLegacy, BluetoothAdapter.LeScanCallback> {

    private final UUIDUtil uuidUtil;
    @Nullable
    private final Set<UUID> filterUuids;

    public LegacyScanOperation(UUID[] filterServiceUUIDs, RxBleAdapterWrapper rxBleAdapterWrapper, final UUIDUtil uuidUtil) {
        super(rxBleAdapterWrapper);

        this.uuidUtil = uuidUtil;
        if (filterServiceUUIDs != null && filterServiceUUIDs.length > 0) {
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

                if (filterUuids == null || uuidUtil.extractUUIDs(scanRecord).containsAll(filterUuids)) {
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

    @Override
    public String toString() {
        return "LegacyScanOperation{"
                + (filterUuids == null ? "" : "ALL_MUST_MATCH -> uuids=" + logUuids(filterUuids))
                + '}';
    }

    private static String logUuids(Set<UUID> filterUuids) {
        int size = filterUuids.size();
        String[] uuids = new String[size];
        Iterator<UUID> iterator = filterUuids.iterator();
        for (int i = 0; i < size; i++) {
            String uuidToLog = LoggerUtil.getUuidToLog(iterator.next());
            uuids[i] = uuidToLog;
        }
        return Arrays.toString(uuids);
    }
}
