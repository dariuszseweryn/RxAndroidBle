package android.os;
import android.util.AndroidException;
/**
 * Used for exceptions
 */
public class RemoteException extends AndroidException {
    public RemoteException() {
        super();
    }
    public RemoteException(String message) {
        super(message);
    }
    public RemoteException(String message, Throwable cause, boolean enableSuppression,
                           boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
    public RemoteException(Throwable cause) {
        this(cause.getMessage(), cause, true, false);
    }
    public RuntimeException rethrowAsRuntimeException() {
        throw new RuntimeException(this);
    }
    public RuntimeException rethrowFromSystemServer() {
        if (this instanceof DeadObjectException) {
            throw new RuntimeException(new DeadSystemException());
        } else {
            throw new RuntimeException(this);
        }
    }
}
