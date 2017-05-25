package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.DeadObjectException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.RadioReleaseInterface;
import com.polidea.rxandroidble.internal.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.internal.util.UUIDUtil;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import rx.Emitter;
import rx.functions.Cancellable;

public class RxBleRadioOperationScan extends RxBleRadioOperation<RxBleInternalScanResult> {

    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final boolean isFilterDefined;
    private final UUIDUtil uuidUtil;
    private final Set<UUID> filterUuids;

    public RxBleRadioOperationScan(UUID[] filterServiceUUIDs, RxBleAdapterWrapper rxBleAdapterWrapper, final UUIDUtil uuidUtil) {

        this.rxBleAdapterWrapper = rxBleAdapterWrapper;

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
    protected void protectedRun(final Emitter<RxBleInternalScanResult> emitter, RadioReleaseInterface radioReleaseInterface) {

        final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

                if (!isFilterDefined || uuidUtil.extractUUIDs(scanRecord).containsAll(filterUuids)) {
                    emitter.onNext(new RxBleInternalScanResult(device, rssi, scanRecord));
                }
            }
        };

        try {
            emitter.setCancellation(new Cancellable() {
                @Override
                public void cancel() throws Exception {
                    rxBleAdapterWrapper.stopLeScan(leScanCallback);
                }
            });

            boolean startLeScanStatus = rxBleAdapterWrapper.startLeScan(leScanCallback);

            if (!startLeScanStatus) {
                emitter.onError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START));
            }
        } catch (Throwable throwable) {
            RxBleLog.e(throwable, "Error while calling BluetoothAdapter.startLeScan()");
            emitter.onError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START));
        } finally {
            radioReleaseInterface.release();
        }
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleScanException(BleScanException.BLUETOOTH_DISABLED, deadObjectException);
    }
}
