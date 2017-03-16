package com.polidea.rxandroidble;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.internal.RxBleLog;

import java.util.Set;
import java.util.UUID;

import rx.Observable;

public abstract class RxBleClient {

    /**
     * Returns instance of RxBleClient using application context. It is required by the client to maintain single instance of RxBleClient.
     *
     * @param context Any Android context
     * @return BLE client instance.
     */
    public static RxBleClient create(@NonNull Context context) {
        return DaggerClientComponent
                .builder()
                .clientModule(new ClientComponent.ClientModule(context))
                .build()
                .rxBleClient();
    }

    /**
     * A convenience method.
     * Sets the log level that will be printed out in the console. Default is LogLevel.NONE which logs nothing.
     *
     * @param logLevel the minimum log level to log
     */
    public static void setLogLevel(@RxBleLog.LogLevel int logLevel) {
        RxBleLog.setLogLevel(logLevel);
    }

    /**
     * Obtain instance of RxBleDevice for provided MAC address. This may be the same instance that was provided during scan operation but
     * this in not guaranteed.
     *
     * @param macAddress Bluetooth LE device MAC address.
     * @return Handle for Bluetooth LE operations.
     */
    public abstract RxBleDevice getBleDevice(@NonNull String macAddress);

    public abstract Set<RxBleDevice> getBondedDevices();

    /**
     * Returns an infinite observable emitting BLE scan results.
     * Scan is automatically started and stopped based on the Observable lifecycle.
     * Scan is started on subscribe and stopped on unsubscribe. You can safely subscribe multiple observers to this observable.
     * <p>
     * The library automatically handles Bluetooth adapter state changes but you are supposed to prompt
     * the user to enable it if it's disabled.
     *
     * @param filterServiceUUIDs Filtering settings. Scan results are only filtered by exported services.
     * @throws com.polidea.rxandroidble.exceptions.BleScanException emits in case of error starting the scan
     */
    public abstract Observable<RxBleScanResult> scanBleDevices(@Nullable UUID... filterServiceUUIDs);
}
