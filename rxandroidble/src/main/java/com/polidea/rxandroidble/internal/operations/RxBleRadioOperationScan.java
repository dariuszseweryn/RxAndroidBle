package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.DeadObjectException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.internal.util.UUIDUtil;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

public class RxBleRadioOperationScan extends RxBleRadioOperation<RxBleInternalScanResult> {

    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private volatile boolean isStarted = false;
    private volatile boolean isStopped = false;

    private final BluetoothAdapter.LeScanCallback leScanCallback;

    public RxBleRadioOperationScan(UUID[] filterServiceUUIDs, RxBleAdapterWrapper rxBleAdapterWrapper, final UUIDUtil uuidUtil) {

        this.rxBleAdapterWrapper = rxBleAdapterWrapper;

        final boolean isFilterDefined = filterServiceUUIDs != null && filterServiceUUIDs.length > 0;
        final HashSet<UUID> filterUuids;
        if (isFilterDefined) {
            filterUuids = new HashSet<>(filterServiceUUIDs.length);
            Collections.addAll(filterUuids, filterServiceUUIDs);
        } else {
            filterUuids = null;
        }

        this.leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

                if (!isFilterDefined || uuidUtil.extractUUIDs(scanRecord).containsAll(filterUuids)) {
                    RxBleRadioOperationScan.this.onNext(new RxBleInternalScanResult(device, rssi, scanRecord));
                }
            }
        };
    }

    @Override
    protected void protectedRun() {

        try {
            boolean startLeScanStatus = rxBleAdapterWrapper.startLeScan(leScanCallback);

            if (!startLeScanStatus) {
                onError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START));
            } else {
                synchronized (this) { // synchronization added for stopping the scan
                    isStarted = true;
                    if (isStopped) {
                        stop();
                    }
                }
            }
        } catch (Throwable throwable) {
            isStarted = true;
            RxBleLog.e(throwable, "Error while calling BluetoothAdapter.startLeScan()");
            onError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START));
        } finally {
            releaseRadio();
        }
    }

    // synchronized keyword added to be sure that operation will be stopped no matter which thread will call it
    public synchronized void stop() {
        isStopped = true;
        if (isStarted) {
            // TODO: [PU] 29.01.2016 https://code.google.com/p/android/issues/detail?id=160503
            rxBleAdapterWrapper.stopLeScan(leScanCallback);
        }
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleScanException(BleScanException.BLUETOOTH_DISABLED, deadObjectException);
    }
}
