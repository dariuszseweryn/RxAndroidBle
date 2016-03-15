package com.polidea.rxandroidble.internal;

import android.support.annotation.IntDef;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This file is a modification of Timber logging library -> https://github.com/JakeWharton/timber
 */
public class RxBleLog {

    @IntDef({VERBOSE, DEBUG, INFO, WARN, ERROR, NONE})
    public @interface LogLevel {

    }

    public static final int VERBOSE = Log.VERBOSE;
    public static final int DEBUG = Log.DEBUG;
    public static final int INFO = Log.INFO;
    public static final int WARN = Log.WARN;
    public static final int ERROR = Log.ERROR;
    public static final int NONE = Integer.MAX_VALUE;
    private static final Pattern ANONYMOUS_CLASS = Pattern.compile("\\$\\d+$");
    private static final ThreadLocal<String> NEXT_TAG = new ThreadLocal<>();
    private static int logLevel = Integer.MAX_VALUE;

    public static void setLogLevel(@LogLevel int logLevel) {
        RxBleLog.logLevel = logLevel;
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
        return "RxBle#" + tag.substring(tag.lastIndexOf('.') + 1);
    }

    private static String formatString(String message, Object... args) {
        // If no varargs are supplied, treat it as a request to log the string without formatting.
        return args.length == 0 ? message : String.format(message, args);
    }

    public static void v(String message, Object... args) {
        throwShade(Log.VERBOSE, formatString(message, args), null);
    }

    public static void v(Throwable t, String message, Object... args) {
        throwShade(Log.VERBOSE, formatString(message, args), t);
    }

    public static void d(String message, Object... args) {
        throwShade(Log.DEBUG, formatString(message, args), null);
    }

    public static void d(Throwable t, String message, Object... args) {
        throwShade(Log.DEBUG, formatString(message, args), t);
    }

    public static void i(String message, Object... args) {
        throwShade(Log.INFO, formatString(message, args), null);
    }

    public static void i(Throwable t, String message, Object... args) {
        throwShade(Log.INFO, formatString(message, args), t);
    }

    public static void w(String message, Object... args) {
        throwShade(Log.WARN, formatString(message, args), null);
    }

    public static void w(Throwable t, String message, Object... args) {
        throwShade(Log.WARN, formatString(message, args), t);
    }

    public static void e(String message, Object... args) {
        throwShade(Log.ERROR, formatString(message, args), null);
    }

    public static void e(Throwable t, String message, Object... args) {
        throwShade(Log.ERROR, formatString(message, args), t);
    }

    private static void throwShade(int priority, String message, Throwable t) {
        if (priority < logLevel) {
            return;
        }

        if (message == null || message.length() == 0) {
            if (t != null) {
                message = Log.getStackTraceString(t);
            } else {
                // Swallow message if it's null and there's no throwable.
                return;
            }
        } else if (t != null) {
            message += "\n" + Log.getStackTraceString(t);
        }

        String tag = createTag();
        println(priority, tag, message);
    }

    private static void println(int priority, String tag, String message) {
        if (message.length() < 4000) {
            Log.println(priority, tag, message);
        } else {
            // It's rare that the message will be this large, so we're ok with the perf hit of splitting
            // and calling Log.println N times.  It's possible but unlikely that a single line will be
            // longer than 4000 characters: we're explicitly ignoring this case here.
            String[] lines = message.split("\n");
            for (String line : lines) {
                Log.println(priority, tag, line);
            }
        }
    }
}
