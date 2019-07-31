package com.polidea.rxandroidble2.internal.operations;


import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.polidea.rxandroidble2.LogConstants;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.internal.logger.LoggerUtil;
import com.polidea.rxandroidble2.internal.scan.AndroidScanObjectsConverter;
import com.polidea.rxandroidble2.internal.scan.EmulatedScanFilterMatcher;
import com.polidea.rxandroidble2.internal.scan.InternalScanResultCreator;
import com.polidea.rxandroidble2.internal.scan.RxBleInternalScanResult;
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.Arrays;
import java.util.List;

import io.reactivex.rxjava3.core.ObservableEmitter;

@RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
public class ScanOperationApi21 extends ScanOperation<RxBleInternalScanResult, ScanCallback> {

    @NonNull
    final InternalScanResultCreator internalScanResultCreator;
    @NonNull
    private final AndroidScanObjectsConverter androidScanObjectsConverter;
    @NonNull
    private final ScanSettings scanSettings;
    @NonNull
    final EmulatedScanFilterMatcher emulatedScanFilterMatcher;
    @Nullable
    private final ScanFilter[] scanFilters;
    @Nullable
    private ObservableEmitter<RxBleInternalScanResult> scanEmitter;


    public ScanOperationApi21(
            @NonNull RxBleAdapterWrapper rxBleAdapterWrapper,
            @NonNull final InternalScanResultCreator internalScanResultCreator,
            @NonNull final AndroidScanObjectsConverter androidScanObjectsConverter,
            @NonNull ScanSettings scanSettings,
            @NonNull final EmulatedScanFilterMatcher emulatedScanFilterMatcher,
            @Nullable final ScanFilter[] offloadedScanFilters
    ) {
        super(rxBleAdapterWrapper);
        this.internalScanResultCreator = internalScanResultCreator;
        this.scanSettings = scanSettings;
        this.emulatedScanFilterMatcher = emulatedScanFilterMatcher;
        this.scanFilters = offloadedScanFilters;
        this.androidScanObjectsConverter = androidScanObjectsConverter;
        scanEmitter = null;
    }

    @Override
    ScanCallback createScanCallback(final ObservableEmitter<RxBleInternalScanResult> emitter) {
        scanEmitter = emitter;
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (!emulatedScanFilterMatcher.isEmpty()
                        && RxBleLog.isAtLeast(LogConstants.DEBUG)
                        && RxBleLog.getShouldLogScannedPeripherals()) {
                    ScanRecord scanRecord = result.getScanRecord();
                    RxBleLog.d("%s, name=%s, rssi=%d, data=%s",
                            LoggerUtil.commonMacMessage(result.getDevice().getAddress()),
                            result.getDevice().getName(),
                            result.getRssi(),
                            LoggerUtil.bytesToHex(scanRecord != null ? scanRecord.getBytes() : null)
                    );
                }
                final RxBleInternalScanResult internalScanResult = internalScanResultCreator.create(callbackType, result);
                if (emulatedScanFilterMatcher.matches(internalScanResult)) {
                    ObservableEmitter<RxBleInternalScanResult> refScanEmitter = scanEmitter;
                    if (refScanEmitter != null) {
                        refScanEmitter.onNext(internalScanResult);
                    }
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult result : results) {
                    final RxBleInternalScanResult internalScanResult = internalScanResultCreator.create(result);
                    if (emulatedScanFilterMatcher.matches(internalScanResult)) {
                        ObservableEmitter<RxBleInternalScanResult> refScanEmitter = scanEmitter;
                        if (refScanEmitter != null) {
                            refScanEmitter.onNext(internalScanResult);
                        }
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                ObservableEmitter<RxBleInternalScanResult> refScanEmitter = scanEmitter;
                if (refScanEmitter != null) {
                    refScanEmitter.tryOnError(new BleScanException(errorCodeToBleErrorCode(errorCode)));
                }
            }
        };
    }

    @Override
    boolean startScan(RxBleAdapterWrapper rxBleAdapterWrapper, ScanCallback scanCallback) {
        if (this.emulatedScanFilterMatcher.isEmpty()) {
            RxBleLog.d("No library side filtering —> debug logs of scanned devices disabled");
        }
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
        if (scanEmitter != null) {
            scanEmitter.onComplete();
            scanEmitter = null;
        }
    }

    @BleScanException.Reason
    static int errorCodeToBleErrorCode(int errorCode) {
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

    @Override
    public String toString() {
        boolean scanFiltersEmpty = scanFilters == null || scanFilters.length == 0;
        boolean emulatedScanFiltersEmpty = emulatedScanFilterMatcher.isEmpty();
        return "ScanOperationApi21{"
                + (scanFiltersEmpty ? "" : "ANY_MUST_MATCH -> nativeFilters=" + Arrays.toString(scanFilters))
                + (!scanFiltersEmpty && !emulatedScanFiltersEmpty ? " and then " : "")
                + (emulatedScanFiltersEmpty ? "" : "ANY_MUST_MATCH -> " + emulatedScanFilterMatcher)
                + '}';
    }
}
