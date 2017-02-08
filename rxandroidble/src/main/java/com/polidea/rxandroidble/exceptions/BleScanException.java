package com.polidea.rxandroidble.exceptions;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Exception emitted as a result of faulty scan operation. The reason describes the situation in details.
 */
public class BleScanException extends BleException {

    /**
     * @hide
     */
    @IntDef({BLUETOOTH_CANNOT_START, BLUETOOTH_DISABLED, BLUETOOTH_NOT_AVAILABLE, LOCATION_PERMISSION_MISSING, LOCATION_SERVICES_DISABLED})
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
     * @return One of {@link #BLUETOOTH_CANNOT_START}, {@link #BLUETOOTH_DISABLED}, {@link #BLUETOOTH_NOT_AVAILABLE},
     * {@link #LOCATION_PERMISSION_MISSING}, {@link #LOCATION_SERVICES_DISABLED}.
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
            default:
                return "UNKNOWN";
        }
    }
}
