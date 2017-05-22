package com.polidea.rxandroidble.internal.operations;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.DeadObjectException;
import android.support.annotation.NonNull;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.scan.EmulatedScanFilterMatcher;
import com.polidea.rxandroidble.internal.scan.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.scan.InternalScanResultCreator;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;

public class RxBleRadioOperationScanApi18 extends RxBleRadioOperationScan {

    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private volatile boolean hasStarted = false;
    private volatile boolean shouldStop = false;

    private final BluetoothAdapter.LeScanCallback leScanCallback;

    public RxBleRadioOperationScanApi18(
            @NonNull RxBleAdapterWrapper rxBleAdapterWrapper,
            @NonNull final InternalScanResultCreator scanResultCreator,
            @NonNull final EmulatedScanFilterMatcher scanFilterMatcher
            ) {

        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        leScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                final RxBleInternalScanResult internalScanResult = scanResultCreator.create(device, rssi, scanRecord);
                if (scanFilterMatcher.matches(internalScanResult)) {
                    onNext(internalScanResult);
                }
            }
        };
    }

    @Override
    protected void protectedRun() {

        try {
            boolean startLeScanStatus = rxBleAdapterWrapper.startLegacyLeScan(leScanCallback);

            if (!startLeScanStatus) {
                onError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START));
            } else {
                synchronized (this) { // synchronization added for stopping the scan
                    hasStarted = true;
                    if (shouldStop) {
                        stop();
                    }
                }
            }
        } catch (Throwable throwable) {
            hasStarted = true;
            RxBleLog.e(throwable, "Error while calling BluetoothAdapter.startLegacyLeScan()");
            onError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START));
        } finally {
            releaseRadio();
        }
    }

    // synchronized keyword added to be sure that operation will be stopped no matter which thread will call it
    public synchronized void stop() {
        shouldStop = true;
        if (hasStarted) {
            // TODO: [PU] 29.01.2016 https://code.google.com/p/android/issues/detail?id=160503
            rxBleAdapterWrapper.stopLegacyLeScan(leScanCallback);
            shouldStop = false;
            hasStarted = false;
        }
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleScanException(BleScanException.BLUETOOTH_DISABLED, deadObjectException);
    }
}
