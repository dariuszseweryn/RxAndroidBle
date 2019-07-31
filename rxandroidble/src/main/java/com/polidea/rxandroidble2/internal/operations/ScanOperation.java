package com.polidea.rxandroidble2.internal.operations;

import android.os.DeadObjectException;

import com.polidea.rxandroidble2.exceptions.BleException;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.internal.QueueOperation;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface;
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper;

import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.functions.Cancellable;

/**
 * A class that handles starting and stopping BLE scans.
 *
 * @param <SCAN_RESULT_TYPE>   Type of the objects that the {@link QueueOperation} should emit
 * @param <SCAN_CALLBACK_TYPE> Type of the BLE scan callback used by a particular implementation
 */
abstract public class ScanOperation<SCAN_RESULT_TYPE, SCAN_CALLBACK_TYPE> extends QueueOperation<SCAN_RESULT_TYPE> {

    final RxBleAdapterWrapper rxBleAdapterWrapper;

    ScanOperation(RxBleAdapterWrapper rxBleAdapterWrapper) {
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
    }

    @Override
    final protected void protectedRun(final ObservableEmitter<SCAN_RESULT_TYPE> emitter, QueueReleaseInterface queueReleaseInterface) {

        final SCAN_CALLBACK_TYPE scanCallback = createScanCallback(emitter);

        try {
            emitter.setCancellable(new Cancellable() {
                @Override
                public void cancel() {
                    RxBleLog.i("Scan operation is requested to stop.");
                    stopScan(rxBleAdapterWrapper, scanCallback);
                }
            });
            RxBleLog.i("Scan operation is requested to start.");
            boolean startLeScanStatus = startScan(rxBleAdapterWrapper, scanCallback);

            if (!startLeScanStatus) {
                emitter.tryOnError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START));
            }
        } catch (Throwable throwable) {
            RxBleLog.w(throwable, "Error while calling the start scan function");
            emitter.tryOnError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START, throwable));
        } finally {
            queueReleaseInterface.release();
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
    abstract SCAN_CALLBACK_TYPE createScanCallback(ObservableEmitter<SCAN_RESULT_TYPE> emitter);

    /**
     * Function that should start the scan using passed {@link RxBleAdapterWrapper} and {@link SCAN_CALLBACK_TYPE} callback
     * @param rxBleAdapterWrapper the {@link RxBleAdapterWrapper} to use
     * @param scanCallback the {@link SCAN_CALLBACK_TYPE} returned by {@link #createScanCallback(ObservableEmitter)} to start
     * @return true if successful
     */
    abstract boolean startScan(RxBleAdapterWrapper rxBleAdapterWrapper, SCAN_CALLBACK_TYPE scanCallback);

    /**
     * Method that should stop the scan for a given {@link SCAN_CALLBACK_TYPE} that was previously returned
     * by {@link #createScanCallback(ObservableEmitter)}
     * @param rxBleAdapterWrapper the {@link RxBleAdapterWrapper} to use
     * @param scanCallback the {@link SCAN_CALLBACK_TYPE} returned by {@link #createScanCallback(ObservableEmitter)} to stop
     */
    abstract void stopScan(RxBleAdapterWrapper rxBleAdapterWrapper, SCAN_CALLBACK_TYPE scanCallback);
}
