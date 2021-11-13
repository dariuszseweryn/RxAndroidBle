package android.util;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
public final class Log {
    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    public static final int ASSERT = 7;
    private Log() {
    }
    public static int v(String tag, String msg) {
        return println(LOG_ID_MAIN, VERBOSE, tag, msg);
    }
    public static int v(String tag, String msg, Throwable tr) {
        return println(LOG_ID_MAIN, VERBOSE, tag, msg + '\n' + getStackTraceString(tr));
    }
    public static int d(String tag, String msg) {
        return println(LOG_ID_MAIN, DEBUG, tag, msg);
    }
    public static int d(String tag, String msg, Throwable tr) {
        return println(LOG_ID_MAIN, DEBUG, tag, msg + '\n' + getStackTraceString(tr));
    }
    public static int i(String tag, String msg) {
        return println(LOG_ID_MAIN, INFO, tag, msg);
    }
    public static int i(String tag, String msg, Throwable tr) {
        return println(LOG_ID_MAIN, INFO, tag, msg + '\n' + getStackTraceString(tr));
    }
    public static int w(String tag, String msg) {
        return println(LOG_ID_MAIN, WARN, tag, msg);
    }
    public static int w(String tag, String msg, Throwable tr) {
        return println(LOG_ID_MAIN, WARN, tag, msg + '\n' + getStackTraceString(tr));
    }
        public static int w(String tag, Throwable tr) {
        return println(LOG_ID_MAIN, WARN, tag, getStackTraceString(tr));
    }
    public static int e(String tag, String msg) {
        return println(LOG_ID_MAIN, ERROR, tag, msg);
    }
    public static int e(String tag, String msg, Throwable tr) {
        return println(LOG_ID_MAIN, ERROR, tag, msg + '\n' + getStackTraceString(tr));
    }
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }
        // This is to reduce the amount of log spew that apps do in the non-error
        // condition of the network being unavailable.
        Throwable t = tr;
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return "";
            }
            t = t.getCause();
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
    public static int println(int priority, String tag, String msg) {
        return println(LOG_ID_MAIN, priority, tag, msg);
    }
     public static final int LOG_ID_MAIN = 0;
     public static final int LOG_ID_RADIO = 1;
     public static final int LOG_ID_EVENTS = 2;
     public static final int LOG_ID_SYSTEM = 3;
     public static final int LOG_ID_CRASH = 4;
     @SuppressWarnings("unused")
    public static int println(int bufID,
                              int priority, String tag, String msg) {
        return 0;
    }
}
