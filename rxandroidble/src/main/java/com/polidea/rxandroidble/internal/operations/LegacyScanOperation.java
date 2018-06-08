package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.internal.eventlog.OperationAttribute;
import com.polidea.rxandroidble.internal.eventlog.OperationDescription;
import com.polidea.rxandroidble.internal.eventlog.OperationEvent;
import com.polidea.rxandroidble.internal.eventlog.OperationEventLogger;
import com.polidea.rxandroidble.internal.eventlog.OperationExtras;
import com.polidea.rxandroidble.internal.scan.RxBleInternalScanResultLegacy;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.internal.util.UUIDUtil;
import com.polidea.rxandroidble.utils.BytePrinter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import bleshadow.javax.inject.Named;

import rx.Emitter;
import rx.Scheduler;
import rx.functions.Action0;

public class LegacyScanOperation extends ScanOperation<RxBleInternalScanResultLegacy, BluetoothAdapter.LeScanCallback> {

    private final boolean isFilterDefined;
    private final UUIDUtil uuidUtil;
    private final OperationEventLogger eventLogger;
    private final Scheduler callbackScheduler;
    private final Set<UUID> filterUuids;
    private Scheduler.Worker callbackSchedulerWorker;

    public LegacyScanOperation(UUID[] filterServiceUUIDs, RxBleAdapterWrapper rxBleAdapterWrapper, final UUIDUtil uuidUtil,
                               OperationEventLogger eventLogger,
                               @Named(ClientComponent.NamedSchedulers.BLUETOOTH_CALLBACKS) Scheduler callbackScheduler) {
        super(rxBleAdapterWrapper, eventLogger);
        this.isFilterDefined = filterServiceUUIDs != null && filterServiceUUIDs.length > 0;
        this.uuidUtil = uuidUtil;
        this.eventLogger = eventLogger;
        this.callbackScheduler = callbackScheduler;

        if (this.isFilterDefined) {
            this.filterUuids = new HashSet<>(filterServiceUUIDs.length);
            Collections.addAll(filterUuids, filterServiceUUIDs);
        } else {
            this.filterUuids = null;
        }
    }

    @Override
    public void onOperationEnqueued() {
        super.onOperationEnqueued();
        callbackSchedulerWorker = callbackScheduler.createWorker();
    }

    @Override
    BluetoothAdapter.LeScanCallback createScanCallback(final Emitter<RxBleInternalScanResultLegacy> emitter) {
        return new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

                if (!isFilterDefined || uuidUtil.extractUUIDs(scanRecord).containsAll(filterUuids)) {
                    final RxBleInternalScanResultLegacy scanResultLegacy = new RxBleInternalScanResultLegacy(device, rssi, scanRecord);

                    callbackSchedulerWorker.schedule(new Action0() {
                        @Override
                        public void call() {
                            logScanResult(scanResultLegacy);
                            emitter.onNext(scanResultLegacy);
                        }
                    });
                }
            }
        };
    }

    private void logScanResult(RxBleInternalScanResultLegacy result) {
        if (eventLogger.isAttached()) {
            final String deviceAddress = result.getBluetoothDevice().getAddress();
            final String deviceName = result.getBluetoothDevice().getName();
            final byte[] scanRecord = result.getScanRecord();
            final String rssi = String.valueOf(result.getRssi());
            final OperationEvent scanResultEvent = new OperationEvent(
                    System.identityHashCode(result),
                    deviceAddress, "SCAN_RESULT", new OperationDescription(
                    new OperationAttribute(OperationExtras.MAC_ADDRESS, deviceAddress),
                    new OperationAttribute(OperationExtras.DEVICE_NAME, (deviceName == null) ? "" : deviceName),
                    new OperationAttribute(OperationExtras.SCAN_RECORD, BytePrinter.toPrettyFormattedHexString(scanRecord)),
                    new OperationAttribute(OperationExtras.RSSI, rssi)
            ));
            eventLogger.onAtomicOperation(scanResultEvent);
        }
    }

    @Override
    boolean startScan(RxBleAdapterWrapper rxBleAdapterWrapper, BluetoothAdapter.LeScanCallback scanCallback) {
        return rxBleAdapterWrapper.startLegacyLeScan(scanCallback);
    }

    @Override
    void stopScan(RxBleAdapterWrapper rxBleAdapterWrapper, BluetoothAdapter.LeScanCallback scanCallback) {
        // TODO: [PU] 29.01.2016 https://code.google.com/p/android/issues/detail?id=160503
        rxBleAdapterWrapper.stopLegacyLeScan(scanCallback);
        callbackSchedulerWorker = null;
    }
}
