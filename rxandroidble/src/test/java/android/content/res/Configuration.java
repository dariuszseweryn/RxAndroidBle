package android.content.res;
import android.os.Parcel;
import android.os.Parcelable;
/**
 * Used for mocks and constants
 */
public final class Configuration implements Parcelable, Comparable<Configuration> {
    public static final int NAVIGATION_UNDEFINED = 0;
    public static final int NAVIGATION_NONAV = 1;
    public static final int NAVIGATION_DPAD = 2;
    public static final int NAVIGATION_TRACKBALL = 3;
    public static final int NAVIGATION_WHEEL = 4;
    public int navigation;
    public Configuration() {
        this.navigation = 0;
    }
    public Configuration(int navigation) {
        this.navigation = navigation;
    }
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
    @Override
    public int compareTo(Configuration o) {
        return 0;
    }
}
