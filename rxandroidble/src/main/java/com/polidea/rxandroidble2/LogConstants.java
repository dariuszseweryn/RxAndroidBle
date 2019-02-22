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

    public static final int VERBOSE = Log.VERBOSE;
    public static final int DEBUG = Log.DEBUG;
    public static final int INFO = Log.INFO;
    public static final int WARN = Log.WARN;
    public static final int ERROR = Log.ERROR;
    public static final int NONE = Integer.MAX_VALUE;

    @IntDef({MAC_ADDRESS_FULL, MAC_ADDRESS_TRUNCATED, NONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MacAddressLogSetting {

    }

    public static final int MAC_ADDRESS_FULL = Log.VERBOSE;
    public static final int MAC_ADDRESS_TRUNCATED = Log.DEBUG;

    @IntDef({UUIDS_FULL, NONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface UuidLogSetting {

    }

    public static final int UUIDS_FULL = Log.VERBOSE;
}
