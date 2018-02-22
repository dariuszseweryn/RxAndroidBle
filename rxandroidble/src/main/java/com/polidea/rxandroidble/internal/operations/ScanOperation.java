package com.polidea.rxandroidble.internal.operations;

import android.os.DeadObjectException;

import com.polidea.rxandroidble.eventlog.OperationAttribute;
import com.polidea.rxandroidble.eventlog.OperationDescription;
import com.polidea.rxandroidble.eventlog.OperationEvent;
import com.polidea.rxandroidble.eventlog.OperationEventLogger;
import com.polidea.rxandroidble.eventlog.OperationExtras;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.QueueOperation;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.scan.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.serialization.QueueReleaseInterface;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.utils.BytePrinter;

import rx.Emitter;
import rx.functions.Cancellable;

/**
 * A class that handles starting and stopping BLE scans.
 *
 * @param <SCAN_RESULT_TYPE>   Type of the objects that the {@link QueueOperation} should emit
 * @param <SCAN_CALLBACK_TYPE> Type of the BLE scan callback used by a particular implementation
 */
abstract public class ScanOperation<SCAN_RESULT_TYPE, SCAN_CALLBACK_TYPE> extends QueueOperation<SCAN_RESULT_TYPE> {

    private static final String SCAN_RESULT_LOG_TITLE = "ScanResult";
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final OperationEventLogger eventLogger;

    ScanOperation(RxBleAdapterWrapper rxBleAdapterWrapper, OperationEventLogger eventLogger) {
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.eventLogger = eventLogger;
    }

    @Override
    final protected void protectedRun(final Emitter<SCAN_RESULT_TYPE> emitter, QueueReleaseInterface queueReleaseInterface) {

        final SCAN_CALLBACK_TYPE scanCallback = createScanCallback(emitter);

        try {
            emitter.setCancellation(new Cancellable() {
                @Override
                public void cancel() throws Exception {
                    RxBleLog.i("Scan operation is requested to stop.");
                    stopScan(rxBleAdapterWrapper, scanCallback);
                }
            });
            RxBleLog.i("Scan operation is requested to start.");
            boolean startLeScanStatus = startScan(rxBleAdapterWrapper, scanCallback);

            if (!startLeScanStatus) {
                emitter.onError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START));
            }
        } catch (Throwable throwable) {
            RxBleLog.e(throwable, "Error while calling the start scan function");
            emitter.onError(new BleScanException(BleScanException.BLUETOOTH_CANNOT_START));
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
    abstract SCAN_CALLBACK_TYPE createScanCallback(Emitter<SCAN_RESULT_TYPE> emitter);

    /**
     * Function that should start the scan using passed {@link RxBleAdapterWrapper} and {@link SCAN_CALLBACK_TYPE} callback
     *
     * @param rxBleAdapterWrapper the {@link RxBleAdapterWrapper} to use
     * @param scanCallback        the {@link SCAN_CALLBACK_TYPE} returned by {@link #createScanCallback(Emitter)} to start
     * @return true if successful
     */
    abstract boolean startScan(RxBleAdapterWrapper rxBleAdapterWrapper, SCAN_CALLBACK_TYPE scanCallback);

    /**
     * Method that should stop the scan for a given {@link SCAN_CALLBACK_TYPE} that was previously returned
     * by {@link #createScanCallback(Emitter)}
     *
     * @param rxBleAdapterWrapper the {@link RxBleAdapterWrapper} to use
     * @param scanCallback        the {@link SCAN_CALLBACK_TYPE} returned by {@link #createScanCallback(Emitter)} to stop
     */
    abstract void stopScan(RxBleAdapterWrapper rxBleAdapterWrapper, SCAN_CALLBACK_TYPE scanCallback);

    void logScanResult(RxBleInternalScanResult result) {
        if (eventLogger.isAttached()) {
            final String deviceAddress = result.getBluetoothDevice().getAddress();
            final String deviceName = result.getScanRecord().getDeviceName();
            final String callbackType = result.getScanCallbackType().name();
            final String rssi = String.valueOf(result.getRssi());
            final OperationEvent scanResultEvent = new OperationEvent(
                    System.identityHashCode(result),
                    deviceAddress, SCAN_RESULT_LOG_TITLE, new OperationDescription(
                    new OperationAttribute(OperationExtras.MAC_ADDRESS, deviceAddress),
                    new OperationAttribute(OperationExtras.DEVICE_NAME, (deviceName == null) ? "" : deviceName),
                    new OperationAttribute(OperationExtras.CALLBACK_TYPE, callbackType),
                    new OperationAttribute(OperationExtras.RSSI, rssi)
            ));
            eventLogger.onAtomicOperation(scanResultEvent, BytePrinter.toPrettyFormattedHexString(result.getScanRecord().getBytes()));
        }
    }
}
