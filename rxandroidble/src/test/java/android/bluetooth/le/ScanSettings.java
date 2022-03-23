package android.bluetooth.le;
import android.os.Parcel;
import android.os.Parcelable;
public class ScanSettings implements Parcelable {
        public static final int SCAN_MODE_OPPORTUNISTIC = -1;
        public static final int SCAN_MODE_LOW_POWER = 0;
        public static final int SCAN_MODE_BALANCED = 1;
        public static final int SCAN_MODE_LOW_LATENCY = 2;
        public static final int SCAN_MODE_AMBIENT_DISCOVERY = 3;
        public static final int CALLBACK_TYPE_ALL_MATCHES = 1;
        public static final int CALLBACK_TYPE_FIRST_MATCH = 2;
        public static final int CALLBACK_TYPE_MATCH_LOST = 4;
            public static final int MATCH_NUM_ONE_ADVERTISEMENT = 1;
        public static final int MATCH_NUM_FEW_ADVERTISEMENT = 2;
        public static final int MATCH_NUM_MAX_ADVERTISEMENT = 3;
        public static final int MATCH_MODE_AGGRESSIVE = 1;
        public static final int MATCH_MODE_STICKY = 2;
        public static final int SCAN_RESULT_TYPE_FULL = 0;
        public static final int SCAN_RESULT_TYPE_ABBREVIATED = 1;
        public static final int PHY_LE_ALL_SUPPORTED = 255;

    public int getScanMode() {
        return 0;
    }
    public int getCallbackType() {
        return 0;
    }
    public int getScanResultType() {
        return 0;
    }
    public int getMatchMode() {
    return 0;
}
    public int getNumOfMatches() {
    return 0;
}
    public boolean getLegacy() {
    return false;
}
    public int getPhy() {
    return 0;
}
    public long getReportDelayMillis() {
        return 0;
    }
    private ScanSettings(int scanMode, int callbackType, int scanResultType,
            long reportDelayMillis, int matchMode,
            int numOfMatchesPerFilter, boolean legacy, int phy) {

    }
    private ScanSettings(Parcel in) {

    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
    @Override
    public int describeContents() {
        return 0;
    }
    public static final Creator<ScanSettings> CREATOR =
            new Creator<ScanSettings>() {
        @Override
        public ScanSettings[] newArray(int size) {
            return new ScanSettings[size];
        }
        @Override
        public ScanSettings createFromParcel(Parcel in) {
            return new ScanSettings(in);
        }
    };
    public static final class Builder {

        public Builder setScanMode(int scanMode) {
            return this;
        }
        public Builder setCallbackType(int callbackType) {
            return this;
        }
        // Returns true if the callbackType is valid.
        private boolean isValidCallbackType(int callbackType) {
            return false;
        }
        public Builder setScanResultType(int scanResultType) {
            return this;
        }
        public Builder setReportDelay(long reportDelayMillis) {
            return this;
        }
        public Builder setNumOfMatches(int numOfMatches) {
            return this;
        }
        public Builder setMatchMode(int matchMode) {
            return this;
        }
        public Builder setLegacy(boolean legacy) {
            return this;
        }
        public Builder setPhy(int phy) {
            return this;
        }
        public ScanSettings build() {
            return new ScanSettings(0, 0, 0, 0, 0, 0, false, 0);
        }
    }
}
