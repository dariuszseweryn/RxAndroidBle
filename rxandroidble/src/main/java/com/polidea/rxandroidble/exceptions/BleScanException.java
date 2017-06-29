package com.polidea.rxandroidble.exceptions;

import android.support.annotation.IntDef;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;

/**
 * Exception emitted as a result of faulty scan operation. The reason describes the situation in details.
 */
public class BleScanException extends BleException {

    @IntDef({BLUETOOTH_CANNOT_START, BLUETOOTH_DISABLED, BLUETOOTH_NOT_AVAILABLE, LOCATION_PERMISSION_MISSING, LOCATION_SERVICES_DISABLED,
            SCAN_FAILED_ALREADY_STARTED, SCAN_FAILED_APPLICATION_REGISTRATION_FAILED, SCAN_FAILED_INTERNAL_ERROR,
            SCAN_FAILED_FEATURE_UNSUPPORTED, SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES, UNDOCUMENTED_SCAN_THROTTLE, UNKNOWN_ERROR_CODE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Reason {

    }

    /**
     * Scan did not start correctly because of unspecified error.
     */
    public static final int BLUETOOTH_CANNOT_START = 0;

    /**
     * Scan did not start correctly because the Bluetooth adapter was disabled. Ask the user to turn on Bluetooth or use
     * <b>android.bluetooth.adapter.action.REQUEST_ENABLE</b>
     */
    public static final int BLUETOOTH_DISABLED = 1;

    /**
     * Scan did not start correctly because the device does not support it.
     */
    public static final int BLUETOOTH_NOT_AVAILABLE = 2;

    /**
     * Scan did not start correctly because the user did not accept access to location services. On Android 6.0 and up you must ask the
     * user about <b>ACCESS_COARSE_LOCATION</b> in runtime.
     */
    public static final int LOCATION_PERMISSION_MISSING = 3;

    /**
     * Scan did not start because location services are disabled on the device. On Android 6.0 and up location services must be enabled
     * in order to receive BLE scan results.
     */
    public static final int LOCATION_SERVICES_DISABLED = 4;

    /**
     * Fails to start scan as BLE scan with the same settings is already started by the app. Only on API >=21.
     */
    public static final int SCAN_FAILED_ALREADY_STARTED = 5;

    /**
     * Fails to start scan as app cannot be registered. Only on API >=21.
     */
    public static final int SCAN_FAILED_APPLICATION_REGISTRATION_FAILED = 6;

    /**
     * Fails to start scan due an internal error. Only on API >=21.
     */
    public static final int SCAN_FAILED_INTERNAL_ERROR = 7;

    /**
     * Fails to start power optimized scan as this feature is not supported. Only on API >=21.
     */
    public static final int SCAN_FAILED_FEATURE_UNSUPPORTED = 8;

    /**
     * Fails to start scan as it is out of hardware resources. Only on API >=21.
     */
    public static final int SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES = 9;

    /**
     * On API >=25 there is an undocumented scan throttling mechanism. If 5 scans were started by the app during a 30 second window
     * the next scan in that window will be silently skipped with only a log warning. In this situation there should be
     * a retryDateSuggestion {@link Date} set with a time when the scan should work again.
     *
     * @link https://blog.classycode.com/undocumented-android-7-ble-behavior-changes-d1a9bd87d983
     */
    public static final int UNDOCUMENTED_SCAN_THROTTLE = Integer.MAX_VALUE - 1;

    /**
     * Unknown error code. Only on API >=21.
     */
    public static final int UNKNOWN_ERROR_CODE = Integer.MAX_VALUE;

    @Reason
    private final int reason;

    private final Date retryDateSuggestion;

    public BleScanException(@Reason int reason) {
        this.reason = reason;
        this.retryDateSuggestion = null;
    }

    public BleScanException(@Reason int reason, @NonNull Date retryDateSuggestion) {
        this.reason = reason;
        this.retryDateSuggestion = retryDateSuggestion;
    }

    public BleScanException(@Reason int reason, Throwable causeException) {
        super(causeException);
        this.reason = reason;
        this.retryDateSuggestion = null;
    }

    /**
     * Returns the reason code of scan failure.
     *
     * @return One of the {@link Reason} codes.
     */
    @Reason
    public int getReason() {
        return reason;
    }

    /**
     * Returns a {@link Date} suggestion when a particular {@link Reason} should no longer be valid
     *
     * @return the date suggestion or null if no suggestion available
     */
    @Nullable
    public Date getRetryDateSuggestion() {
        return retryDateSuggestion;
    }

    @Override
    public String toString() {
        return "BleScanException{"
                + "reason=" + reasonDescription()
                + retryDateSuggestionIfExists()
                + toStringCauseIfExists()
                + '}';
    }

    private String reasonDescription() {
        switch (reason) {
            case BLUETOOTH_CANNOT_START:
                return "BLUETOOTH_CANNOT_START";
            case BLUETOOTH_DISABLED:
                return "BLUETOOTH_DISABLED";
            case BLUETOOTH_NOT_AVAILABLE:
                return "BLUETOOTH_NOT_AVAILABLE";
            case LOCATION_PERMISSION_MISSING:
                return "LOCATION_PERMISSION_MISSING";
            case LOCATION_SERVICES_DISABLED:
                return "LOCATION_SERVICES_DISABLED";
            case SCAN_FAILED_ALREADY_STARTED:
                return "SCAN_FAILED_ALREADY_STARTED";
            case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                return "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED";
            case SCAN_FAILED_INTERNAL_ERROR:
                return "SCAN_FAILED_INTERNAL_ERROR";
            case SCAN_FAILED_FEATURE_UNSUPPORTED:
                return "SCAN_FAILED_FEATURE_UNSUPPORTED";
            case SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES:
                return "SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES";
            case UNDOCUMENTED_SCAN_THROTTLE:
                return "UNDOCUMENTED_SCAN_THROTTLE";
            case UNKNOWN_ERROR_CODE:
                // fallthrough
            default:
                return "UNKNOWN";
        }
    }

    private String retryDateSuggestionIfExists() {
        if (retryDateSuggestion == null) {
            return "";
        } else {
            return ", retryDateSuggestion=" + retryDateSuggestion;
        }
    }
}
