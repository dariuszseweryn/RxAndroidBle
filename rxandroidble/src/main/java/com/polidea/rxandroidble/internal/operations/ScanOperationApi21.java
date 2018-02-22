package com.polidea.rxandroidble.internal.operations;


import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.eventlog.OperationAttribute;
import com.polidea.rxandroidble.eventlog.OperationDescription;
import com.polidea.rxandroidble.eventlog.OperationEvent;
import com.polidea.rxandroidble.eventlog.OperationEventLogger;
import com.polidea.rxandroidble.eventlog.OperationExtras;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.scan.AndroidScanObjectsConverter;
import com.polidea.rxandroidble.internal.scan.EmulatedScanFilterMatcher;
import com.polidea.rxandroidble.internal.scan.InternalScanResultCreator;
import com.polidea.rxandroidble.internal.scan.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanSettings;

import java.util.List;

import bleshadow.javax.inject.Named;
import rx.Emitter;
import rx.Scheduler;
import rx.functions.Action0;

import static com.polidea.rxandroidble.eventlog.OperationEvent.operationIdentifierHash;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScanOperationApi21 extends ScanOperation<RxBleInternalScanResult, ScanCallback> {

    private static final String LOG_TITLE = "Scan request";
    @NonNull
    private final InternalScanResultCreator internalScanResultCreator;
    @NonNull
    private final AndroidScanObjectsConverter androidScanObjectsConverter;
    @NonNull
    private final OperationEventLogger eventLogger;
    @NonNull
    private final Scheduler callbackScheduler;
    @NonNull
    private final ScanSettings scanSettings;
    @NonNull
    private final EmulatedScanFilterMatcher emulatedScanFilterMatcher;
    @Nullable
    private final ScanFilter[] scanFilters;
    private Scheduler.Worker scanResultSchedulerWorker;

    public ScanOperationApi21(
            @NonNull RxBleAdapterWrapper rxBleAdapterWrapper,
            @NonNull final InternalScanResultCreator internalScanResultCreator,
            @NonNull final AndroidScanObjectsConverter androidScanObjectsConverter,
            @NonNull ScanSettings scanSettings,
            @NonNull final EmulatedScanFilterMatcher emulatedScanFilterMatcher,
            @Nullable final ScanFilter[] offloadedScanFilters,
            @NonNull final OperationEventLogger eventLogger,
            @NonNull @Named(ClientComponent.NamedSchedulers.BLUETOOTH_CALLBACKS) Scheduler callbackScheduler
    ) {
        super(rxBleAdapterWrapper, eventLogger);
        this.internalScanResultCreator = internalScanResultCreator;
        this.scanSettings = scanSettings;
        this.emulatedScanFilterMatcher = emulatedScanFilterMatcher;
        this.scanFilters = offloadedScanFilters;
        this.androidScanObjectsConverter = androidScanObjectsConverter;
        this.eventLogger = eventLogger;
        this.callbackScheduler = callbackScheduler;
    }

    @Override
    public void onOperationEnqueued() {
        super.onOperationEnqueued();
        eventLogger.onOperationEnqueued(
                new OperationEvent(operationIdentifierHash(this), LOG_TITLE, getClass().getSimpleName(),
                        new OperationDescription(
                                new OperationAttribute(OperationExtras.SCAN_TYPE, "ScanOperationApi21"),
                                new OperationAttribute(OperationExtras.SCAN_MODE, String.valueOf(scanSettings.getScanMode())),
                                new OperationAttribute(OperationExtras.CALLBACK_TYPE, String.valueOf(scanSettings.getCallbackType())),
                                new OperationAttribute(OperationExtras.REPORT_DELAY, String.valueOf(scanSettings.getReportDelayMillis())),
                                new OperationAttribute(OperationExtras.MATCH_MODE, String.valueOf(scanSettings.getNumOfMatches())),
                                new OperationAttribute(OperationExtras.NUM_OF_MATCHES, String.valueOf(scanSettings.getNumOfMatches()))
                        )));

        scanResultSchedulerWorker = callbackScheduler.createWorker();
    }

    @Override
    ScanCallback createScanCallback(final Emitter<RxBleInternalScanResult> emitter) {
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                final RxBleInternalScanResult internalScanResult = internalScanResultCreator.create(callbackType, result);

                if (emulatedScanFilterMatcher.matches(internalScanResult)) {
                    scanResultSchedulerWorker.schedule(new Action0() {
                        @Override
                        public void call() {
                            logScanResult(internalScanResult);
                            emitter.onNext(internalScanResult);
                        }
                    });
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult result : results) {
                    final RxBleInternalScanResult internalScanResult = internalScanResultCreator.create(result);

                    if (emulatedScanFilterMatcher.matches(internalScanResult)) {
                        scanResultSchedulerWorker.schedule(new Action0() {
                            @Override
                            public void call() {
                                logScanResult(internalScanResult);
                                emitter.onNext(internalScanResult);
                            }
                        });
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                final BleScanException bleScanException = new BleScanException(errorCodeToBleErrorCode(errorCode));
                eventLogger.onOperationFailed(
                        new OperationEvent(operationIdentifierHash(ScanOperationApi21.this), LOG_TITLE, getClass().getSimpleName()),
                        bleScanException.toString());
                emitter.onError(bleScanException);
            }
        };
    }

    @Override
    boolean startScan(RxBleAdapterWrapper rxBleAdapterWrapper, ScanCallback scanCallback) {
        eventLogger.onOperationStarted(new OperationEvent(operationIdentifierHash(this), LOG_TITLE, getClass().getSimpleName()));
        rxBleAdapterWrapper.startLeScan(
                androidScanObjectsConverter.toNativeFilters(scanFilters),
                androidScanObjectsConverter.toNativeSettings(scanSettings),
                scanCallback
        );
        return true;
    }

    @Override
    void stopScan(RxBleAdapterWrapper rxBleAdapterWrapper, ScanCallback scanCallback) {
        rxBleAdapterWrapper.stopLeScan(scanCallback);
        eventLogger.onOperationFinished(new OperationEvent(operationIdentifierHash(this), LOG_TITLE, getClass().getSimpleName()));

        if (scanResultSchedulerWorker != null) {
            scanResultSchedulerWorker.unsubscribe();
            scanResultSchedulerWorker = null;
        }
    }

    @BleScanException.Reason
    private static int errorCodeToBleErrorCode(int errorCode) {
        switch (errorCode) {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                return BleScanException.SCAN_FAILED_ALREADY_STARTED;
            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                return BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED;
            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                return BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED;
            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                return BleScanException.SCAN_FAILED_INTERNAL_ERROR;
            case 5: // ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES
                return BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES;
            default:
                RxBleLog.w("Encountered unknown scanning error code: %d -> check android.bluetooth.le.ScanCallback");
                return BleScanException.UNKNOWN_ERROR_CODE;
        }
    }
}
