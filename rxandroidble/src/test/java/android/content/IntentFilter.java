package android.content;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.AndroidException;
/**
 * Instances created, so must have implementation of action
 */
public class IntentFilter implements Parcelable {

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    public static class MalformedMimeTypeException extends AndroidException {
        public MalformedMimeTypeException() {
        }
        public MalformedMimeTypeException(String name) {
            super(name);
        }
    }
    public static IntentFilter create(String action, String dataType) {
        try {
            return new IntentFilter(action, dataType);
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("Bad MIME type", e);
        }
    }
    String mAction;
    public IntentFilter(String action, String dataType)
            throws MalformedMimeTypeException {
        mAction = action;
    }
    public IntentFilter(String action)
            throws MalformedMimeTypeException {
        mAction = action;

    }
    public final boolean hasAction(String action) {
        return action.equals(mAction);
    }


}
