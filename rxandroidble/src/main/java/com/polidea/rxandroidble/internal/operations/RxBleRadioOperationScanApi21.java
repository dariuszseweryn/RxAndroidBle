package com.polidea.rxandroidble.internal.operations;


import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
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
import rx.Emitter;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RxBleRadioOperationScanApi21 extends RxBleRadioOperationScan<RxBleInternalScanResult, ScanCallback> {

    @NonNull
    private final InternalScanResultCreator internalScanResultCreator;
    @NonNull
    private final AndroidScanObjectsConverter androidScanObjectsConverter;
    @NonNull
    private final ScanSettings scanSettings;
    @NonNull
    private final EmulatedScanFilterMatcher emulatedScanFilterMatcher;
    @Nullable
    private final ScanFilter[] scanFilters;


    public RxBleRadioOperationScanApi21(
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
    }

    @Override
    ScanCallback createScanCallback(final Emitter<RxBleInternalScanResult> emitter) {
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                final RxBleInternalScanResult internalScanResult = internalScanResultCreator.create(callbackType, result);
                if (emulatedScanFilterMatcher.matches(internalScanResult)) {
                    emitter.onNext(internalScanResult);
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult result : results) {
                    final RxBleInternalScanResult internalScanResult = internalScanResultCreator.create(result);
                    if (emulatedScanFilterMatcher.matches(internalScanResult)) {
                        emitter.onNext(internalScanResult);
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                emitter.onError(new BleScanException(errorCodeToBleErrorCode(errorCode)));
            }
        };
    }

    @Override
    boolean startScan(RxBleAdapterWrapper rxBleAdapterWrapper, ScanCallback scanCallback) {
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
    }

    @BleScanException.Reason private static int errorCodeToBleErrorCode(int errorCode) {
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
