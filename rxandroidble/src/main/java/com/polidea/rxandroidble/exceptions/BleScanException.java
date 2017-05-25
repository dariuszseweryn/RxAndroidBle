package com.polidea.rxandroidble.exceptions;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Exception emitted as a result of faulty scan operation. The reason describes the situation in details.
 */
public class BleScanException extends BleException {

    @IntDef({BLUETOOTH_CANNOT_START, BLUETOOTH_DISABLED, BLUETOOTH_NOT_AVAILABLE, LOCATION_PERMISSION_MISSING, LOCATION_SERVICES_DISABLED,
            SCAN_FAILED_ALREADY_STARTED, SCAN_FAILED_APPLICATION_REGISTRATION_FAILED, SCAN_FAILED_INTERNAL_ERROR,
            SCAN_FAILED_FEATURE_UNSUPPORTED, SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES, UNKNOWN_ERROR_CODE})
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
     * Unknown error code. Only on API >=21.
     */
    public static final int UNKNOWN_ERROR_CODE = Integer.MAX_VALUE;

    @Reason
    private final int reason;

    public BleScanException(@Reason int reason) {
        this.reason = reason;
    }

    public BleScanException(@Reason int reason, Throwable causeException) {
        super(causeException);
        this.reason = reason;
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

    @Override
    public String toString() {
        return "BleScanException{"
                + "reason=" + reasonDescription()
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
            case UNKNOWN_ERROR_CODE:
                // fallthrough
            default:
                return "UNKNOWN";
        }
    }
}
