package com.polidea.rxandroidble2.internal.operations;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.polidea.rxandroidble2.LogConstants;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.internal.logger.LoggerUtil;
import com.polidea.rxandroidble2.internal.scan.RxBleInternalScanResultLegacy;
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import io.reactivex.ObservableEmitter;

public class LegacyScanOperation extends ScanOperation<RxBleInternalScanResultLegacy, BluetoothAdapter.LeScanCallback> {

    @SuppressWarnings("deprecation")
    final com.polidea.rxandroidble2.internal.util.UUIDUtil uuidUtil;
    @Nullable
    final Set<UUID> filterUuids;

    @SuppressWarnings("deprecation")
    public LegacyScanOperation(UUID[] filterServiceUUIDs,
                               RxBleAdapterWrapper rxBleAdapterWrapper,
                               final com.polidea.rxandroidble2.internal.util.UUIDUtil uuidUtil) {
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
    BluetoothAdapter.LeScanCallback createScanCallback(final ObservableEmitter<RxBleInternalScanResultLegacy> emitter) {
        return new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                if (filterUuids != null && RxBleLog.isAtLeast(LogConstants.DEBUG)) {
                    RxBleLog.d("%s, name=%s, rssi=%d, data=%s",
                            LoggerUtil.commonMacMessage(device.getAddress()),
                            device.getName(),
                            rssi,
                            LoggerUtil.bytesToHex(scanRecord)
                    );
                }
                if (filterUuids == null || uuidUtil.extractUUIDs(scanRecord).containsAll(filterUuids)) {
                    emitter.onNext(new RxBleInternalScanResultLegacy(device, rssi, scanRecord));
                }
            }
        };
    }

    @Override
    boolean startScan(RxBleAdapterWrapper rxBleAdapterWrapper, BluetoothAdapter.LeScanCallback scanCallback) {
        if (this.filterUuids == null) {
            RxBleLog.d("No library side filtering —> debug logs of scanned devices disabled");
        }
        return rxBleAdapterWrapper.startLegacyLeScan(scanCallback);
    }

    @Override
    void stopScan(RxBleAdapterWrapper rxBleAdapterWrapper, BluetoothAdapter.LeScanCallback scanCallback) {
        // TODO: [PU] 29.01.2016 https://code.google.com/p/android/issues/detail?id=160503
        rxBleAdapterWrapper.stopLegacyLeScan(scanCallback);
    }

    @Override
    @NonNull
    public String toString() {
        return "LegacyScanOperation{"
                + (filterUuids == null ? "" : "ALL_MUST_MATCH -> uuids=" + LoggerUtil.getUuidSetToLog(filterUuids))
                + '}';
    }
}
