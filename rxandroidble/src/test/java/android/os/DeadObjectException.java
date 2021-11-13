package android.os;
/**
 * Used for exceptions
 */
public class DeadObjectException extends RemoteException {
    public DeadObjectException() {
        super();
    }
    public DeadObjectException(String message) {
        super(message);
    }
}
