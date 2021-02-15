package com.polidea.rxandroidble3.internal;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import android.util.Log;

import com.polidea.rxandroidble3.LogConstants;
import com.polidea.rxandroidble3.LogOptions;

import com.polidea.rxandroidble3.internal.logger.LoggerSetup;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This file is a modification of Timber logging library -> https://github.com/JakeWharton/timber
 */
public class RxBleLog {

    @IntDef({VERBOSE, DEBUG, INFO, WARN, ERROR, NONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface LogLevel {

    }

    @Deprecated
    public static final int VERBOSE = Log.VERBOSE;
    @Deprecated
    public static final int DEBUG = Log.DEBUG;
    @Deprecated
    public static final int INFO = Log.INFO;
    @Deprecated
    public static final int WARN = Log.WARN;
    @Deprecated
    public static final int ERROR = Log.ERROR;
    @Deprecated
    public static final int NONE = Integer.MAX_VALUE;

    private static final Pattern ANONYMOUS_CLASS = Pattern.compile("\\$\\d+$");
    private static final ThreadLocal<String> NEXT_TAG = new ThreadLocal<>();

    private static final LogOptions.Logger LOGCAT_LOGGER = new LogOptions.Logger() {
        @Override
        public void log(final int level, final String tag, final String msg) {
            Log.println(level, tag, msg);
        }
    };

    private static LoggerSetup loggerSetup = new LoggerSetup(
            LogConstants.NONE,
            LogConstants.NONE,
            LogConstants.NONE,
            false,
            true,
            LOGCAT_LOGGER
    );

    private RxBleLog() {

    }

    /**
     * Simple logging interface for log messages from RxAndroidBle
     *
     * @see #setLogger(Logger)
     * @deprecated use {@link com.polidea.rxandroidble3.RxBleClient#updateLogOptions(LogOptions)}
     */
    public interface Logger {

        /**
         * @param level one of {@link Log#VERBOSE}, {@link Log#DEBUG},{@link Log#INFO},
         *              {@link Log#WARN},{@link Log#ERROR}
         * @param tag   log tag, caller
         * @param msg   message to log
         */
        void log(int level, String tag, String msg);
    }

    /**
     * Old method to set a custom logger implementation, set it to {@code null} to use default logcat logging.
     * It updates only the logger object. The rest of log settings remain unchanged.
     * <p>
     * Example how to forward logs to Timber:<br>
     *
     * <code>
     * <pre>
     * RxBleLog.setLogger(new RxBleLog.Logger() {
     *    &#64;Override
     *    public void log(final int level, final String tag, final String msg) {
     *        Timber.tag(tag).log(level, msg);
     *    }
     * });
     * </pre>
     * </code>
     *
     * @deprecated use {@link com.polidea.rxandroidble3.RxBleClient#updateLogOptions(LogOptions)}
     */
    @Deprecated
    public static void setLogger(@Nullable final Logger logger) {
        LogOptions.Logger loggerToSet = logger == null
                ? LOGCAT_LOGGER
                : new LogOptions.Logger() {
            @Override
            public void log(int level, String tag, String msg) {
                logger.log(level, tag, msg);
            }
        };
        LogOptions newLogOptions = new LogOptions.Builder().setLogger(loggerToSet).build();
        RxBleLog.updateLogOptions(newLogOptions);
    }

    /**
     * Old method to set log level. It updates only the log level value. The rest of log settings remain unchanged.
     *
     * @param logLevel the log level
     * @deprecated use {@link com.polidea.rxandroidble3.RxBleClient#updateLogOptions(LogOptions)}
     */
    @Deprecated
    public static void setLogLevel(@LogLevel int logLevel) {
        LogOptions newLogOptions = new LogOptions.Builder().setLogLevel(logLevel).build();
        updateLogOptions(newLogOptions);
    }

    /**
     * Method to update current logger setup with new LogOptions. Only set options will be updated. Options that were not set or set to null
     * on the LogOptions will not update the current setup leaving the previous values untouched.
     *
     * @param logOptions the new log options
     */
    public static void updateLogOptions(LogOptions logOptions) {
        LoggerSetup oldLoggerSetup = RxBleLog.loggerSetup;
        LoggerSetup newLoggerSetup = oldLoggerSetup.merge(logOptions);
        d("Received new options (%s) and merged with old setup: %s. New setup: %s", logOptions, oldLoggerSetup, newLoggerSetup);
        RxBleLog.loggerSetup = newLoggerSetup;
    }

    private static String createTag() {
        String tag = NEXT_TAG.get();
        if (tag != null) {
            NEXT_TAG.remove();
            return tag;
        }

        StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        if (stackTrace.length < 5) {
            throw new IllegalStateException(
                    "Synthetic stacktrace didn't have enough elements: are you using proguard?");
        }
        tag = stackTrace[4].getClassName();
        Matcher m = ANONYMOUS_CLASS.matcher(tag);
        if (m.find()) {
            tag = m.replaceAll("");
        }
        tag = tag.replace("Impl", "");
        tag = tag.replace("RxBle", "");
        int endIndex = tag.indexOf('$');
        String classTag = endIndex <= 0
                ? tag.substring(tag.lastIndexOf('.') + 1)
                : tag.substring(tag.lastIndexOf('.') + 1, endIndex);
        return "RxBle#" + classTag;
    }

    private static String formatString(String message, Object... args) {
        // If no varargs are supplied, treat it as a request to log the string without formatting.
        return args.length == 0 ? message : String.format(message, args);
    }

    public static void v(String message, Object... args) {
        throwShade(Log.VERBOSE, null, message, args);
    }

    public static void v(Throwable t, String message, Object... args) {
        throwShade(Log.VERBOSE, t, message, args);
    }

    public static void d(String message, Object... args) {
        throwShade(Log.DEBUG, null, message, args);
    }

    public static void d(Throwable t, String message, Object... args) {
        throwShade(Log.DEBUG, t, message, args);
    }

    public static void i(String message, Object... args) {
        throwShade(Log.INFO, null, message, args);
    }

    public static void i(Throwable t, String message, Object... args) {
        throwShade(Log.INFO, t, message, args);
    }

    public static void w(String message, Object... args) {
        throwShade(Log.WARN, null, message, args);
    }

    public static void w(Throwable t, String message, Object... args) {
        throwShade(Log.WARN, t, message, args);
    }

    public static void e(String message, Object... args) {
        throwShade(Log.ERROR, null, message, args);
    }

    public static void e(Throwable t, String message, Object... args) {
        throwShade(Log.ERROR, t, message, args);
    }

    private static void throwShade(int priority, Throwable t, String message, Object... args) {
        if (priority < loggerSetup.logLevel) {
            return;
        }

        final String formattedMessage = formatString(message, args);
        final String finalMessage;

        if (formattedMessage == null || formattedMessage.length() == 0) {
            if (t != null) {
                finalMessage = Log.getStackTraceString(t);
            } else {
                // Swallow message if it's null and there's no throwable.
                return;
            }
        } else if (t != null) {
            finalMessage = formattedMessage + "\n" + Log.getStackTraceString(t);
        } else {
            finalMessage = formattedMessage;
        }

        String tag = createTag();
        println(priority, tag, finalMessage);
    }

    private static void println(int priority, String tag, String message) {
        if (message.length() < 4000) {
            loggerSetup.logger.log(priority, tag, message);
        } else {
            // It's rare that the message will be this large, so we're ok with the perf hit of splitting
            // and calling Log.println N times.  It's possible but unlikely that a single line will be
            // longer than 4000 characters: we're explicitly ignoring this case here.
            String[] lines = message.split("\n");
            for (String line : lines) {
                loggerSetup.logger.log(priority, tag, line);
            }
        }
    }

    public static boolean isAtLeast(int expectedLogLevel) {
        return loggerSetup.logLevel <= expectedLogLevel;
    }

    public static @LogConstants.MacAddressLogSetting int getMacAddressLogSetting() {
        return loggerSetup.macAddressLogSetting;
    }

    public static @LogConstants.UuidLogSetting int getUuidLogSetting() {
        return loggerSetup.uuidLogSetting;
    }

    public static boolean getShouldLogAttributeValues() {
        return loggerSetup.shouldLogAttributeValues;
    }

    public static boolean getShouldLogScannedPeripherals() {
        return loggerSetup.shouldLogScannedPeripherals;
    }
}
