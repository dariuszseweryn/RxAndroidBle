package android.os;
public class DeadObjectException extends RemoteException {
    public DeadObjectException() {
        super();
    }
    public DeadObjectException(String message) {
        super(message);
    }
}
