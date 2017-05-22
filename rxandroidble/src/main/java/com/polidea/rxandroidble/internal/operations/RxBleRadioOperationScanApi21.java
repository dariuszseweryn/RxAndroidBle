package com.polidea.rxandroidble.internal.operations;


import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.DeadObjectException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.scan.EmulatedScanFilterMatcher;
import com.polidea.rxandroidble.internal.scan.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.scan.AndroidScanObjectsConverter;
import com.polidea.rxandroidble.internal.scan.InternalScanResultCreator;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanSettings;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RxBleRadioOperationScanApi21 extends RxBleRadioOperationScan {

    private final ScanSettings scanSettings;
    private final ScanFilter[] scanFilters;
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private volatile boolean isStarted = false;
    private volatile boolean isStopped = false;

    private final ScanCallback scanCallback;
    private final AndroidScanObjectsConverter androidScanObjectsConverter;

    public RxBleRadioOperationScanApi21(
            @NonNull RxBleAdapterWrapper rxBleAdapterWrapper,
            @NonNull final InternalScanResultCreator internalScanResultCreator,
            @NonNull final AndroidScanObjectsConverter androidScanObjectsConverter,
            @NonNull ScanSettings scanSettings,
            @NonNull final EmulatedScanFilterMatcher emulatedScanFilterMatcher,
            @Nullable final ScanFilter[] offloadedScanFilters
    ) {
        this.scanSettings = scanSettings;
        this.scanFilters = offloadedScanFilters;
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.androidScanObjectsConverter = androidScanObjectsConverter;
        this.scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                final RxBleInternalScanResult internalScanResult = internalScanResultCreator.create(callbackType, result);
                if (emulatedScanFilterMatcher.matches(internalScanResult)) {
                    onNext(internalScanResult);
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult result : results) {
                    final RxBleInternalScanResult internalScanResult = internalScanResultCreator.create(result);
                    if (emulatedScanFilterMatcher.matches(internalScanResult)) {
                        onNext(internalScanResult);
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                onError(new BleScanException(errorCodeToBleErrorCode(errorCode)));
            }
        };
    }

    @Override
    protected void protectedRun() {

        try {
            rxBleAdapterWrapper.startLeScan(
                    androidScanObjectsConverter.toNativeFilters(scanFilters),
                    androidScanObjectsConverter.toNativeSettings(scanSettings),
                    scanCallback
            );
        } catch (Throwable e) {
            onError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START, e));
        }

        synchronized (this) { // synchronization added for stopping the scan
            isStarted = true;
            if (isStopped) {
                stop();
            }
        }

        releaseRadio();
    }

    // synchronized keyword added to be sure that operation will be stopped no matter which thread will call it
    public synchronized void stop() {
        isStopped = true;
        if (isStarted) {
            rxBleAdapterWrapper.stopLeScan(scanCallback);
        }
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleScanException(BleScanException.BLUETOOTH_DISABLED, deadObjectException);
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
