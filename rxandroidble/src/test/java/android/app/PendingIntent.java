package android.app;
import android.os.Parcel;
import android.os.Parcelable;
public class PendingIntent implements Parcelable {
    public PendingIntent() {
    }
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
