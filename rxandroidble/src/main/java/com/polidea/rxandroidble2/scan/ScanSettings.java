package com.polidea.rxandroidble2.scan;


import android.bluetooth.le.BluetoothLeScanner;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.IntDef;
import com.polidea.rxandroidble2.internal.scan.ExternalScanSettingsExtension;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Bluetooth LE scan settings are passed to {@link BluetoothLeScanner#startScan} to define the
 * parameters for the scan.
 *
 * RxAndroidBle Note: This class is basically copied from the Android AOSP. All of the exposed functionality
 * is emulated by the software due to a lot of potential issues with different phones:
 * https://code.google.com/p/android/issues/detail?id=158522
 * https://code.google.com/p/android/issues/detail?id=178614
 * https://code.google.com/p/android/issues/detail?id=228428
 */
public class ScanSettings implements Parcelable, ExternalScanSettingsExtension<ScanSettings> {

    @IntDef({SCAN_MODE_OPPORTUNISTIC, SCAN_MODE_LOW_POWER, SCAN_MODE_BALANCED, SCAN_MODE_LOW_LATENCY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScanMode {

    }

    @IntDef(value = {CALLBACK_TYPE_ALL_MATCHES, CALLBACK_TYPE_FIRST_MATCH, CALLBACK_TYPE_MATCH_LOST}, flag = true)
    @Retention(RetentionPolicy.SOURCE)
    public @interface CallbackType {

    }

    @IntDef({MATCH_NUM_ONE_ADVERTISEMENT, MATCH_NUM_FEW_ADVERTISEMENT, MATCH_NUM_MAX_ADVERTISEMENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MatchNum {

    }

    @IntDef({MATCH_MODE_AGGRESSIVE, MATCH_MODE_STICKY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MatchMode {

    }

    /**
     * A special Bluetooth LE scan mode. Applications using this scan mode will passively listen for
     * other scan results without starting BLE scans themselves.
     */
    public static final int SCAN_MODE_OPPORTUNISTIC = -1;

    /**
     * Perform Bluetooth LE scan in low power mode. This is the default scan mode as it consumes the
     * least power.
     */
    public static final int SCAN_MODE_LOW_POWER = 0;

    /**
     * Perform Bluetooth LE scan in balanced power mode. Scan results are returned at a rate that
     * provides a good trade-off between scan frequency and power consumption.
     */
    public static final int SCAN_MODE_BALANCED = 1;

    /**
     * Scan using highest duty cycle. It's recommended to only use this mode when the application is
     * running in the foreground.
     */
    public static final int SCAN_MODE_LOW_LATENCY = 2;

    /**
     * Trigger a callback for every Bluetooth advertisement found that matches the filter criteria.
     * If no filter is active, all advertisement packets are reported.
     */
    public static final int CALLBACK_TYPE_ALL_MATCHES = 1;

    /**
     * A result callback is only triggered for the first advertisement packet received that matches
     * the filter criteria.
     */
    public static final int CALLBACK_TYPE_FIRST_MATCH = 2;

    /**
     * Receive a callback when advertisements are no longer received from a device that has been
     * previously reported by a first match callback.
     */
    public static final int CALLBACK_TYPE_MATCH_LOST = 4;


    /**
     * Determines how many advertisements to match per filter, as this is scarce hw resource
     */
    /**
     * Match one advertisement per filter
     */
    public static final int MATCH_NUM_ONE_ADVERTISEMENT = 1;

    /**
     * Match few advertisement per filter, depends on current capability and availability of
     * the resources in hw
     */
    public static final int MATCH_NUM_FEW_ADVERTISEMENT = 2;

    /**
     * Match as many advertisement per filter as hw could allow, depends on current
     * capability and availability of the resources in hw
     */
    public static final int MATCH_NUM_MAX_ADVERTISEMENT = 3;


    /**
     * In Aggressive mode, hw will determine a match sooner even with feeble signal strength
     * and few number of sightings/match in a duration.
     */
    public static final int MATCH_MODE_AGGRESSIVE = 1;

    /**
     * For sticky mode, higher threshold of signal strength and sightings is required
     * before reporting by hw
     */
    public static final int MATCH_MODE_STICKY = 2;

    // Bluetooth LE scan mode.
    @ScanMode
    private int mScanMode;

    // Bluetooth LE scan callback type
    @CallbackType
    private int mCallbackType;

    // Time of delay for reporting the scan result
    private long mReportDelayMillis;

    @MatchMode
    private int mMatchMode;

    @MatchNum
    private int mNumOfMatchesPerFilter;

    private boolean mShouldCheckLocationProviderState;

    @ScanMode
    public int getScanMode() {
        return mScanMode;
    }

    @CallbackType
    public int getCallbackType() {
        return mCallbackType;
    }

    @MatchMode
    public int getMatchMode() {
        return mMatchMode;
    }

    @MatchNum
    public int getNumOfMatches() {
        return mNumOfMatchesPerFilter;
    }

    /**
     * Returns report delay timestamp based on the device clock.
     */
    public long getReportDelayMillis() {
        return mReportDelayMillis;
    }

    @Override
    public boolean shouldCheckLocationProviderState() {
        return mShouldCheckLocationProviderState;
    }

    ScanSettings(int scanMode, int callbackType,
                 long reportDelayMillis, int matchMode, int numOfMatchesPerFilter, boolean shouldCheckLocationServicesState) {
        mScanMode = scanMode;
        mCallbackType = callbackType;
        mReportDelayMillis = reportDelayMillis;
        mNumOfMatchesPerFilter = numOfMatchesPerFilter;
        mMatchMode = matchMode;
        mShouldCheckLocationProviderState = shouldCheckLocationServicesState;
    }

    ScanSettings(Parcel in) {
        //noinspection WrongConstant
        mScanMode = in.readInt();
        //noinspection WrongConstant
        mCallbackType = in.readInt();
        mReportDelayMillis = in.readLong();
        //noinspection WrongConstant
        mMatchMode = in.readInt();
        //noinspection WrongConstant
        mNumOfMatchesPerFilter = in.readInt();
        mShouldCheckLocationProviderState = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mScanMode);
        dest.writeInt(mCallbackType);
        dest.writeLong(mReportDelayMillis);
        dest.writeInt(mMatchMode);
        dest.writeInt(mNumOfMatchesPerFilter);
        dest.writeInt(mShouldCheckLocationProviderState ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ScanSettings>
            CREATOR = new Creator<ScanSettings>() {
        @Override
        public ScanSettings[] newArray(int size) {
            return new ScanSettings[size];
        }

        @Override
        public ScanSettings createFromParcel(Parcel in) {
            return new ScanSettings(in);
        }
    };

    @Override
    public ScanSettings copyWithCallbackType(@CallbackType int callbackType) {
        return new ScanSettings(
                this.mScanMode,
                callbackType,
                this.mReportDelayMillis,
                this.mMatchMode,
                this.mNumOfMatchesPerFilter,
                this.mShouldCheckLocationProviderState
        );
    }

    /**
     * Builder for {@link ScanSettings}.
     */
    public static final class Builder implements ExternalScanSettingsExtension.Builder {

        private int mScanMode = SCAN_MODE_LOW_POWER;
        private int mCallbackType = CALLBACK_TYPE_ALL_MATCHES;
        private long mReportDelayMillis = 0;
        private int mMatchMode = MATCH_MODE_AGGRESSIVE;
        private int mNumOfMatchesPerFilter = MATCH_NUM_MAX_ADVERTISEMENT;
        private boolean mShouldCheckLocationProviderState = true;

        /**
         * Set scan mode for Bluetooth LE scan.
         *
         * @param scanMode The scan mode can be one of {@link ScanSettings#SCAN_MODE_LOW_POWER},
         *                 {@link ScanSettings#SCAN_MODE_BALANCED} or
         *                 {@link ScanSettings#SCAN_MODE_LOW_LATENCY}.
         * @throws IllegalArgumentException If the {@code scanMode} is invalid.
         */
        public ScanSettings.Builder setScanMode(@ScanMode int scanMode) {
            if (scanMode < SCAN_MODE_OPPORTUNISTIC || scanMode > SCAN_MODE_LOW_LATENCY) {
                throw new IllegalArgumentException("invalid scan mode " + scanMode);
            }
            mScanMode = scanMode;
            return this;
        }

        /**
         * Set callback type for Bluetooth LE scan.
         *
         * @param callbackType The callback type flags for the scan.
         * @throws IllegalArgumentException If the {@code callbackType} is invalid.
         */
        public ScanSettings.Builder setCallbackType(@CallbackType int callbackType) {

            if (!isValidCallbackType(callbackType)) {
                throw new IllegalArgumentException("invalid callback type - " + callbackType);
            }
            mCallbackType = callbackType;
            return this;
        }

        /**
         * {@inheritDoc}
         * If set to true and Location Services are off a {@link com.polidea.rxandroidble2.exceptions.BleScanException} will be emitted.
         * <p>Default: true.</p>
         */
        @Override
        public ScanSettings.Builder setShouldCheckLocationServicesState(boolean shouldCheck) {
            mShouldCheckLocationProviderState = shouldCheck;
            return this;
        }

        // Returns true if the callbackType is valid.
        private static boolean isValidCallbackType(int callbackType) {
            if (callbackType == CALLBACK_TYPE_ALL_MATCHES
                    || callbackType == CALLBACK_TYPE_FIRST_MATCH
                    || callbackType == CALLBACK_TYPE_MATCH_LOST) {
                return true;
            }
            return callbackType == (CALLBACK_TYPE_FIRST_MATCH | CALLBACK_TYPE_MATCH_LOST);
        }

        // [DS 27.04.2017] TODO: when there will be a need
//        /**
//         * Set report delay timestamp for Bluetooth LE scan.
//         *
//         * @param reportDelayMillis Delay of report in milliseconds. Set to 0 to be notified of
//         *                          results immediately. Values &gt; 0 causes the scan results to be queued up and
//         *                          delivered after the requested delay or when the internal buffers fill up.
//         * @throws IllegalArgumentException If {@code reportDelayMillis} &lt; 0.
//         */
//        public ScanSettings.Builder setReportDelay(long reportDelayMillis) {
//            if (reportDelayMillis < 0) {
//                throw new IllegalArgumentException("reportDelay must be > 0");
//            }
//            mReportDelayMillis = reportDelayMillis;
//            return this;
//        }
//
//        /**
//         * Set the number of matches for Bluetooth LE scan filters hardware match
//         *
//         * @param numOfMatches The num of matches can be one of
//         *                     {@link ScanSettings#MATCH_NUM_ONE_ADVERTISEMENT} or
//         *                     {@link ScanSettings#MATCH_NUM_FEW_ADVERTISEMENT} or
//         *                     {@link ScanSettings#MATCH_NUM_MAX_ADVERTISEMENT}
//         * @throws IllegalArgumentException If the {@code matchMode} is invalid.
//         */
//        public ScanSettings.Builder setNumOfMatches(@MatchNum int numOfMatches) {
//            if (numOfMatches < MATCH_NUM_ONE_ADVERTISEMENT
//                    || numOfMatches > MATCH_NUM_MAX_ADVERTISEMENT) {
//                throw new IllegalArgumentException("invalid numOfMatches " + numOfMatches);
//            }
//            mNumOfMatchesPerFilter = numOfMatches;
//            return this;
//        }
//
//        /**
//         * Set match mode for Bluetooth LE scan filters hardware match
//         *
//         * @param matchMode The match mode can be one of
//         *                  {@link ScanSettings#MATCH_MODE_AGGRESSIVE} or
//         *                  {@link ScanSettings#MATCH_MODE_STICKY}
//         * @throws IllegalArgumentException If the {@code matchMode} is invalid.
//         */
//        public ScanSettings.Builder setMatchMode(@MatchMode int matchMode) {
//            if (matchMode < MATCH_MODE_AGGRESSIVE
//                    || matchMode > MATCH_MODE_STICKY) {
//                throw new IllegalArgumentException("invalid matchMode " + matchMode);
//            }
//            mMatchMode = matchMode;
//            return this;
//        }

        /**
         * Build {@link ScanSettings}.
         */
        public ScanSettings build() {
            return new ScanSettings(mScanMode, mCallbackType,
                    mReportDelayMillis, mMatchMode, mNumOfMatchesPerFilter, mShouldCheckLocationProviderState);
        }
    }
}