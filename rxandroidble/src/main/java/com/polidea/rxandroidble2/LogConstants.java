package com.polidea.rxandroidble2;

import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;

public final class LogConstants {

    private LogConstants() {
    }

    @IntDef({VERBOSE, DEBUG, INFO, WARN, ERROR, NONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LogLevel {

    }

    /**
     * {@link #DEBUG} + some internal library logs
     */
    public static final int VERBOSE = Log.VERBOSE;
    /**
     * {@link #INFO} + info needed to debug the library if a wrong behaviour is observed
     */
    public static final int DEBUG = Log.DEBUG;
    /**
     * {@link #WARN} + info needed to understand what is happening with the Android OS BLE interactions
     */
    public static final int INFO = Log.INFO;
    /**
     * {@link #ERROR} + warnings that are handled gracefully or API misuse will be logged
     */
    public static final int WARN = Log.WARN;
    /**
     * Only critical library errors will be logged
     */
    public static final int ERROR = Log.ERROR;
    /**
     * Nothing will be logged in the respective setting
     * <p>Log Level — nothing will be logged no matter the other setting
     * <p>MAC Address — will be logged as 'XX:XX:XX:XX:XX:XX'
     * <p>UUIDs — will be logged as '...'
     */
    public static final int NONE = Integer.MAX_VALUE;

    @IntDef({MAC_ADDRESS_FULL, MAC_ADDRESS_TRUNCATED, NONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MacAddressLogSetting {

    }

    /**
     * Full MAC address will be logged i.e. '00:11:22:33:44:55'
     */
    public static final int MAC_ADDRESS_FULL = Log.VERBOSE;
    /**
     * Truncated MAC address will be logged i.e. '00:11:22:33:44:XX'
     */
    public static final int MAC_ADDRESS_TRUNCATED = Log.DEBUG;

    @IntDef({UUIDS_FULL, NONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface UuidLogSetting {

    }

    /**
     * Full UUID will be logged
     */
    public static final int UUIDS_FULL = Log.VERBOSE;
}
