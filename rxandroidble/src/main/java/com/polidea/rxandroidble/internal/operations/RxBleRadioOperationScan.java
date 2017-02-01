package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothAdapter;

import android.bluetooth.BluetoothDevice;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.internal.util.UUIDUtil;

import java.util.List;
import java.util.UUID;

public class RxBleRadioOperationScan extends RxBleRadioOperation<RxBleInternalScanResult> {

    private final UUID[] filterServiceUUIDs;
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final UUIDUtil uuidUtil;
    private volatile boolean isStarted = false;
    private volatile boolean isStopped = false;

    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            if (!RxBleRadioOperationScan.this.hasDefinedFilter()
                    || RxBleRadioOperationScan.this.hasDefinedFilter()
                    && RxBleRadioOperationScan.this.containsDesiredServiceIds(scanRecord)) {
                RxBleRadioOperationScan.this.onNext(new RxBleInternalScanResult(device, rssi, scanRecord));
            }
        }
    };

    public RxBleRadioOperationScan(UUID[] filterServiceUUIDs, RxBleAdapterWrapper rxBleAdapterWrapper, UUIDUtil uuidUtil) {

        this.filterServiceUUIDs = filterServiceUUIDs;
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.uuidUtil = uuidUtil;
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
