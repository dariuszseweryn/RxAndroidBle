package com.polidea.rxandroidble2.internal.scan;


import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.exceptions.BleScanException;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

import io.reactivex.rxjava3.core.Scheduler;

public class ScanPreconditionsVerifierApi24 implements ScanPreconditionsVerifier {

    /*
     * default values taken from
     * https://android.googlesource.com/platform/packages/apps/Bluetooth/+/android-7.0.0_r1/src/com/android/bluetooth/gatt/AppScanStats.java
     */
    private static final int SCANS_LENGTH = 5;
    private static final long EXCESSIVE_SCANNING_PERIOD = TimeUnit.SECONDS.toMillis(30);
    private final long[] previousChecks = new long[SCANS_LENGTH];
    private final ScanPreconditionsVerifierApi18 scanPreconditionVerifierApi18;
    private final Scheduler timeScheduler;

    @Inject
    public ScanPreconditionsVerifierApi24(
            ScanPreconditionsVerifierApi18 scanPreconditionVerifierApi18,
            @Named(ClientComponent.NamedSchedulers.COMPUTATION) Scheduler timeScheduler
            ) {
        this.scanPreconditionVerifierApi18 = scanPreconditionVerifierApi18;
        this.timeScheduler = timeScheduler;
    }

    @Override
    public void verify(boolean checkLocationProviderState) {
        scanPreconditionVerifierApi18.verify(checkLocationProviderState);

        /*
         * Android 7.0 (API 24) introduces an undocumented scan throttle for applications that try to scan more than 5 times during
         * a 30 second window. More on the topic: https://blog.classycode.com/undocumented-android-7-ble-behavior-changes-d1a9bd87d983
         */

        // TODO: [DS] 27.06.2017 Think if persisting this information through Application close is needed
        final int oldestCheckTimestampIndex = getOldestCheckTimestampIndex();
        final long oldestCheckTimestamp = previousChecks[oldestCheckTimestampIndex];
        final long currentCheckTimestamp = timeScheduler.now(TimeUnit.MILLISECONDS);

        if (currentCheckTimestamp - oldestCheckTimestamp < EXCESSIVE_SCANNING_PERIOD) {
            throw new BleScanException(
                    BleScanException.UNDOCUMENTED_SCAN_THROTTLE,
                    new Date(oldestCheckTimestamp + EXCESSIVE_SCANNING_PERIOD)
            );
        }
        previousChecks[oldestCheckTimestampIndex] = currentCheckTimestamp;
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
