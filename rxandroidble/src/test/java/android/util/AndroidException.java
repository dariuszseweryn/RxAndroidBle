package android.util;
/**
 * Used for exceptions
 */
public class AndroidException extends Exception {
    public AndroidException() {
    }
    public AndroidException(String name) {
        super(name);
    }
    public AndroidException(String name, Throwable cause) {
        super(name, cause);
    }
    public AndroidException(Exception cause) {
        super(cause);
    }
    protected AndroidException(String message, Throwable cause, boolean enableSuppression,
                               boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
};
