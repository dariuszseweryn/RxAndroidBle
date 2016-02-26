package com.polidea.rxandroidble;

import android.content.Context;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.internal.RxBleLog;

import java.util.UUID;

import rx.Observable;

public interface RxBleClient {

    /**
     * A convenience method.
     * Sets the log level that will be printed out in the console. Default is LogLevel.NONE which logs nothing.
     * @param logLevel the minimum log level to log
     */
    static void setLogLevel(@RxBleLog.LogLevel int logLevel) {
        RxBleLog.setLogLevel(logLevel);
    }

    static RxBleClient createInstance(Context context) {
        return new RxBleClientImpl(context);
    }

    RxBleDevice getBleDevice(String bluetoothAddress);

    /**
     * Returns an infinite observable returning BLE scan results. Scan is automatically started and stopped based on the Observable lifecycle.
     *
     * @param filterServiceUUIDs Filtering settings. Scan results are only filtered by exported services.
     * @throws com.polidea.rxandroidble.exceptions.BleScanException         in case of error starting the scan
     * @throws com.polidea.rxandroidble.exceptions.BleNotAvailableException in case of Bluetooth not available or not enabled
     */
    Observable<RxBleScanResult> scanBleDevices(
            @Nullable UUID[] filterServiceUUIDs
    );
}
