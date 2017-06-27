package com.polidea.rxandroidble.internal.scan;


import android.support.annotation.Nullable;
import java.util.Date;

public class ExcessiveScanCheckerApi24 implements ExcessiveScanChecker {

    /*
     * default values taken from
     * https://android.googlesource.com/platform/packages/apps/Bluetooth/+/android-7.0.0_r1/src/com/android/bluetooth/gatt/AppScanStats.java
     */
    private static final int SCANS_LENGTH = 5;

    private static final long EXCESSIVE_SCANNING_PERIOD = 30 * 1000;

    private final long[] previousChecks = new long[SCANS_LENGTH];

    @Nullable
    @Override
    public Date suggestDateToRetry() {
        final int oldestCheckTimestampIndex = getOldestCheckTimestampIndex();
        final long oldestCheckTimestamp = previousChecks[oldestCheckTimestampIndex];
        final long currentCheckTimestamp = System.currentTimeMillis();
        if (currentCheckTimestamp - oldestCheckTimestamp < EXCESSIVE_SCANNING_PERIOD) {
            return new Date(oldestCheckTimestamp + EXCESSIVE_SCANNING_PERIOD);
        }
        previousChecks[oldestCheckTimestampIndex] = currentCheckTimestamp;
        return null;
    }

    private int getOldestCheckTimestampIndex() {
        long oldestTimestamp = Long.MAX_VALUE;
        int index = -1;
        for (int i = 0; i < SCANS_LENGTH; i++) {
            final long previousCheckTimestamp = previousChecks[i];
            if (previousCheckTimestamp < oldestTimestamp) {
                index = i;
                oldestTimestamp = previousCheckTimestamp;
            }
        }
        return index;
    }
}
