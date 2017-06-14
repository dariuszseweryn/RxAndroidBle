package com.polidea.rxandroidble.internal.operations;

import android.os.DeadObjectException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.RadioReleaseInterface;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import rx.Emitter;
import rx.functions.Cancellable;

/**
 * A class that handles starting and stopping BLE scans.
 *
 * @param <SCAN_RESULT_TYPE>   Type of the objects that the RxBleRadioOperation should emit
 * @param <SCAN_CALLBACK_TYPE> Type of the BLE scan callback used by a particular implementation
 */
abstract public class RxBleRadioOperationScan<SCAN_RESULT_TYPE, SCAN_CALLBACK_TYPE> extends RxBleRadioOperation<SCAN_RESULT_TYPE> {

    private final RxBleAdapterWrapper rxBleAdapterWrapper;

    RxBleRadioOperationScan(RxBleAdapterWrapper rxBleAdapterWrapper) {
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
    }

    @Override
    final protected void protectedRun(final Emitter<SCAN_RESULT_TYPE> emitter, RadioReleaseInterface radioReleaseInterface) {

        final SCAN_CALLBACK_TYPE scanCallback = createScanCallback(emitter);

        try {
            emitter.setCancellation(new Cancellable() {
                @Override
                public void cancel() throws Exception {
                    stopScan(rxBleAdapterWrapper, scanCallback);
                }
            });

            boolean startLeScanStatus = startScan(rxBleAdapterWrapper, scanCallback);

            if (!startLeScanStatus) {
                emitter.onError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START));
            }
        } catch (Throwable throwable) {
            RxBleLog.e(throwable, "Error while calling the start scan function");
            emitter.onError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START));
        } finally {
            radioReleaseInterface.release();
        }
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleScanException(BleScanException.BLUETOOTH_DISABLED, deadObjectException);
    }

    /**
     * Function that should return a scan callback of a proper type. The returned scan callback will later be passed
     * to {@link #startScan(RxBleAdapterWrapper, Object)} and {@link #stopScan(RxBleAdapterWrapper, Object)}
     *
     * @param emitter the emitter for notifications
     * @return the scan callback type to use with {@link #startScan(RxBleAdapterWrapper, Object)}
     * and {@link #stopScan(RxBleAdapterWrapper, Object)}
     */
    abstract SCAN_CALLBACK_TYPE createScanCallback(Emitter<SCAN_RESULT_TYPE> emitter);

    /**
     * Function that should start the scan using passed {@link RxBleAdapterWrapper} and {@link SCAN_CALLBACK_TYPE} callback
     * @param rxBleAdapterWrapper the {@link RxBleAdapterWrapper} to use
     * @param scanCallback the {@link SCAN_CALLBACK_TYPE} returned by {@link #createScanCallback(Emitter)} to start
     * @return true if successful
     */
    abstract boolean startScan(RxBleAdapterWrapper rxBleAdapterWrapper, SCAN_CALLBACK_TYPE scanCallback);

    /**
     * Method that should stop the scan for a given {@link SCAN_CALLBACK_TYPE} that was previously returned
     * by {@link #createScanCallback(Emitter)}
     * @param rxBleAdapterWrapper the {@link RxBleAdapterWrapper} to use
     * @param scanCallback the {@link SCAN_CALLBACK_TYPE} returned by {@link #createScanCallback(Emitter)} to stop
     */
    abstract void stopScan(RxBleAdapterWrapper rxBleAdapterWrapper, SCAN_CALLBACK_TYPE scanCallback);
}
