package com.polidea.rxandroidble.internal.operations;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.eventlog.OperationAttribute;
import com.polidea.rxandroidble.eventlog.OperationDescription;
import com.polidea.rxandroidble.eventlog.OperationEvent;
import com.polidea.rxandroidble.eventlog.OperationEventLogger;
import com.polidea.rxandroidble.eventlog.OperationExtras;
import com.polidea.rxandroidble.internal.scan.EmulatedScanFilterMatcher;
import com.polidea.rxandroidble.internal.scan.InternalScanResultCreator;
import com.polidea.rxandroidble.internal.scan.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;

import bleshadow.javax.inject.Named;
import rx.Emitter;
import rx.Scheduler;
import rx.functions.Action0;

import static com.polidea.rxandroidble.eventlog.OperationEvent.operationIdentifierHash;

public class ScanOperationApi18 extends ScanOperation<RxBleInternalScanResult, BluetoothAdapter.LeScanCallback> {

    private static final String LOG_TITLE = "Scan request";
    @NonNull
    private final InternalScanResultCreator scanResultCreator;
    @NonNull
    private final EmulatedScanFilterMatcher scanFilterMatcher;
    @NonNull
    private final OperationEventLogger eventLogger;
    @NonNull
    private final Scheduler callbackScheduler;
    private Scheduler.Worker scanResultSchedulerWorker;

    public ScanOperationApi18(
            @NonNull RxBleAdapterWrapper rxBleAdapterWrapper,
            @NonNull final InternalScanResultCreator scanResultCreator,
            @NonNull final EmulatedScanFilterMatcher scanFilterMatcher,
            @NonNull final OperationEventLogger eventLogger,
            @NonNull @Named(ClientComponent.NamedSchedulers.BLUETOOTH_CALLBACKS) Scheduler callbackScheduler
    ) {

        super(rxBleAdapterWrapper, eventLogger);
        this.scanResultCreator = scanResultCreator;
        this.scanFilterMatcher = scanFilterMatcher;
        this.eventLogger = eventLogger;
        this.callbackScheduler = callbackScheduler;
    }

    @Override
    public void onOperationEnqueued() {
        super.onOperationEnqueued();
        eventLogger.onOperationEnqueued(
                new OperationEvent(operationIdentifierHash(this), LOG_TITLE, getClass().getSimpleName(),
                        new OperationDescription(new OperationAttribute(OperationExtras.SCAN_TYPE, getClass().getSimpleName()))
                ));
        scanResultSchedulerWorker = callbackScheduler.createWorker();
    }

    @Override
    BluetoothAdapter.LeScanCallback createScanCallback(final Emitter<RxBleInternalScanResult> emitter) {
        return new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

                final RxBleInternalScanResult internalScanResult = scanResultCreator.create(device, rssi, scanRecord);
                if (scanFilterMatcher.matches(internalScanResult)) {
                    scanResultSchedulerWorker.schedule(new Action0() {
                        @Override
                        public void call() {
                            logScanResult(internalScanResult);
                            emitter.onNext(internalScanResult);
                        }
                    });
                }
            }
        };
    }

    @Override
    boolean startScan(RxBleAdapterWrapper rxBleAdapterWrapper, BluetoothAdapter.LeScanCallback scanCallback) {
        final boolean operationStarted = rxBleAdapterWrapper.startLegacyLeScan(scanCallback);
        final OperationEvent event = new OperationEvent(operationIdentifierHash(this), LOG_TITLE, getClass().getSimpleName());

        if (operationStarted) {
            eventLogger.onOperationStarted(event);
        } else {
            eventLogger.onOperationFailed(event, "Unable to start scan.");
        }

        return operationStarted;
    }

    @Override
    void stopScan(RxBleAdapterWrapper rxBleAdapterWrapper, BluetoothAdapter.LeScanCallback scanCallback) {
        // TODO: [PU] 29.01.2016 https://code.google.com/p/android/issues/detail?id=160503
        rxBleAdapterWrapper.stopLegacyLeScan(scanCallback);
        eventLogger.onOperationFinished(new OperationEvent(operationIdentifierHash(this), LOG_TITLE, getClass().getSimpleName()));

        if (scanResultSchedulerWorker != null) {
            scanResultSchedulerWorker.unsubscribe();
            scanResultSchedulerWorker = null;
        }
    }
}
