package com.polidea.rxandroidble2;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.scan.BackgroundScanner;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.Set;
import java.util.UUID;

import io.reactivex.rxjava3.core.Observable;


public abstract class RxBleClient {

    @SuppressWarnings("WeakerAccess")
    public enum State {
        /**
         * Bluetooth Adapter is not available on the given OS. Most functions will throw {@link UnsupportedOperationException} when called.
         */
        BLUETOOTH_NOT_AVAILABLE,
        /**
         * Location permission is not given. Scanning and connecting to a device will not work. Used on API >=23.
         */
        LOCATION_PERMISSION_NOT_GRANTED,
        /**
         * Bluetooth Adapter is not switched on. Scanning and connecting to a device will not work.
         */
        BLUETOOTH_NOT_ENABLED,
        /**
         * Location Services are switched off. Scanning will not work. Used on API >=23.
         */
        LOCATION_SERVICES_NOT_ENABLED,
        /**
         * Everything is ready to be used.
         */
        READY
    }

    /**
     * Returns instance of RxBleClient using application context. It is required by the client to maintain single instance of RxBleClient.
     *
     * @param context Any Android context
     * @return BLE client instance.
     */
    public static RxBleClient create(@NonNull Context context) {
        return DaggerClientComponent
                .builder()
                .applicationContext(context.getApplicationContext())
                .build()
                .rxBleClient();
    }

    /**
     * A convenience method.
     * Sets the log level that will be printed out in the console. Default is LogLevel.NONE which logs nothing.
     *
     * @deprecated use {@link #updateLogOptions(LogOptions)}
     * @param logLevel the minimum log level to log
     */
    @Deprecated
    public static void setLogLevel(@RxBleLog.LogLevel int logLevel) {
        RxBleLog.setLogLevel(logLevel);
    }

    /**
     * Method for updating logging options. Properties not set in {@link LogOptions} will not get updated.
     * <p>
     *     Default behaviour: MAC addresses are not logged (MAC='XX:XX:XX:XX:XX:XX'), uuids are not logged (uuid='...'), byte array values
     *     are not logged (value=[...]), logger is logging to the logcat ({@link android.util.Log}), all scanned peripherals are logged if
     *     log level allows it, log level is set not to log anything ({@link LogConstants#NONE})
     * </p>
     *
     * @param logOptions the logging options
     */
    public static void updateLogOptions(LogOptions logOptions) {
        RxBleLog.updateLogOptions(logOptions);
    }

    /**
     * Obtain instance of RxBleDevice for provided MAC address. This may be the same instance that was provided during scan operation but
     * this in not guaranteed.
     *
     * @param macAddress Bluetooth LE device MAC address.
     * @return Handle for Bluetooth LE operations.
     * @throws UnsupportedOperationException if called on system without Bluetooth capabilities
     */
    public abstract RxBleDevice getBleDevice(@NonNull String macAddress);

    /**
     * A function returning a set of currently bonded devices
     *
     * If Bluetooth state is not STATE_ON, this API will return an empty set. After turning on Bluetooth, wait for ACTION_STATE_CHANGED
     * with STATE_ON to get the updated value.
     *
     * @return set of currently bonded devices
     * @throws UnsupportedOperationException if called on system without Bluetooth capabilities
     */
    public abstract Set<RxBleDevice> getBondedDevices();

    /**
     * Returns an infinite observable emitting BLE scan results.
     * Scan is automatically started and stopped based on the Observable lifecycle.
     * Scan is started when the Observable is subscribed and stopped when unsubscribed.
     * You can safely subscribe multiple observers to this observable.
     * When defining filterServiceUUIDs have in mind that the {@link RxBleScanResult} will be emitted only if _all_ UUIDs are present
     * in the advertisement.
     * <p>
     * The library automatically handles Bluetooth adapter's state changes but you are supposed to prompt
     * the user to enable it if it's disabled.
     *
     * @param filterServiceUUIDs Filtering settings. Scan results are only filtered by exported services.
     *                           All specified UUIDs must be present in the advertisement data to match the filter.
     * @throws com.polidea.rxandroidble2.exceptions.BleScanException emits in case of error starting the scan
     * @deprecated use {@link #scanBleDevices(ScanSettings, ScanFilter...)} instead
     */
    @Deprecated
    public abstract Observable<RxBleScanResult> scanBleDevices(@Nullable UUID... filterServiceUUIDs);

    /**
     * Returns an infinite observable emitting BLE scan results.
     * Scan is automatically started and stopped based on the Observable lifecycle.
     * Scan is started on subscribe and stopped on unsubscribe. You can safely subscribe multiple observers to this observable.
     * <p>
     * The library automatically handles Bluetooth adapter state changes but you are supposed to prompt the user
     * to enable it if it is disabled
     *
     * This function works on Android 4.3 in compatibility (emulated) mode.
     *
     * @param scanSettings Scan settings
     * @param scanFilters Filtering settings. ScanResult will be emitted if <i>any</i> of the passed scan filters will match.
     */
    public abstract Observable<ScanResult> scanBleDevices(ScanSettings scanSettings, ScanFilter... scanFilters);

    /**
     * Returns a background scanner instance that can be used to handle background scans, even if your process is stopped.
     */
    public abstract BackgroundScanner getBackgroundScanner();

    /**
     * Returns an observable emitting state _changes_ of the RxBleClient environment which may be helpful in deciding if particular
     * functionality should be used at a given moment.
     *
     * @see #getState() for {@link State} precedence order
     *
     * Examples:
     * - If the device is in {@link State#READY} and the user will turn off the bluetooth adapter then {@link State#BLUETOOTH_NOT_ENABLED}
     * will be emitted.
     * - If the device is in {@link State#BLUETOOTH_NOT_ENABLED} then changing state of Location Services will not cause emissions
     * because of the checks order
     * - If the device is in {@link State#BLUETOOTH_NOT_AVAILABLE} then this {@link Observable} will complete because any other checks
     * will not be performed as devices are not expected to obtain bluetooth capabilities during runtime
     *
     * To get the initial {@link State} and then observe changes you can use: `observeStateChanges().startWith(getState())`.
     *
     * @return the observable
     */
    public abstract Observable<State> observeStateChanges();

    /**
     * Returns the current state of the RxBleClient environment, which may be helpful in deciding if particular functionality
     * should be used at a given moment. The function concentrates on states that are blocking the full functionality of the library.
     *
     * Checking order:
     * 1. Is Bluetooth available?
     * 2. Is Location Permission granted? (if needed = API>=23)
     * 3. Is Bluetooth Adapter on?
     * 4. Are Location Services enabled? (if needed = API>=23)
     *
     * If any of the checks fails an appropriate State is returned and next checks are not performed.
     *
     * State precedence order is as follows:
     * {@link State#BLUETOOTH_NOT_AVAILABLE} if check #1 fails,
     * {@link State#LOCATION_PERMISSION_NOT_GRANTED} if check #2 fails,
     * {@link State#BLUETOOTH_NOT_ENABLED} if check #3 fails,
     * {@link State#LOCATION_SERVICES_NOT_ENABLED} if check #4 fails,
     * {@link State#READY}
     *
     * @return the current state
     */
    public abstract State getState();

    /**
     * Returns whether runtime permissions needed to run a BLE scan are granted. If permissions are not granted then one may check
     * {@link #getRecommendedScanRuntimePermissions()} to get Android runtime permission strings needed for running a scan.
     *
     * @return true if needed permissions are granted, false otherwise
     */
    public abstract boolean isScanRuntimePermissionGranted();

    /**
     * Returns permission strings needed by the application to run a BLE scan or an empty array if no runtime permissions are needed. Since
     * Android 6.0 runtime permissions were introduced. To run a BLE scan a runtime permission is needed ever since. Since Android 10.0
     * a different (finer) permission is needed. Only a single permission returned by this function is needed to perform a scan. It is up
     * to the user to decide which one. The result array is sorted with the least permissive values first.
     * <p>
     * Returned values:
     * <p>
     * case: API < 23<p>
     * Empty array. No runtime permissions needed.
     * <p>
     * case: 23 <= API < 29<p>
     * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}
     * <p>
     * case: 29 <= API<p>
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}
     *
     * @return an ordered array of possible scan permissions
     */
    public abstract String[] getRecommendedScanRuntimePermissions();
}
