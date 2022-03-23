package com.polidea.rxandroidble2.internal.scan;


import android.Manifest;
import android.content.pm.PackageInfo;

import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.internal.util.LocationServicesStatus;
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import io.reactivex.Scheduler;

public class ScanPreconditionsVerifierApi31 implements ScanPreconditionsVerifier {

    /*
     * default values taken from
     * https://android.googlesource.com/platform/packages/apps/Bluetooth/+/android-7.0.0_r1/src/com/android/bluetooth/gatt/AppScanStats.java
     */
    private static final int SCANS_LENGTH = 5;
    private static final long EXCESSIVE_SCANNING_PERIOD = TimeUnit.SECONDS.toMillis(30);
    private final long[] previousChecks = new long[SCANS_LENGTH];
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final LocationServicesStatus locationServicesStatus;
    private final Scheduler timeScheduler;
    private final PackageInfo packageInfo;

    @Inject
    public ScanPreconditionsVerifierApi31(
            RxBleAdapterWrapper rxBleAdapterWrapper,
            LocationServicesStatus locationServicesStatus,
            @Named(ClientComponent.NamedSchedulers.COMPUTATION) Scheduler timeScheduler,
            @Named(ClientComponent.PlatformConstants.PACKAGE_INFO) PackageInfo packageInfo
            ) {
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.locationServicesStatus = locationServicesStatus;
        this.timeScheduler = timeScheduler;
        this.packageInfo = packageInfo;
    }

    @Override
    public void verify(boolean checkLocationProviderState) {
        // determine if we really need to check location
        if (checkLocationProviderState
                && this.packageInfo != null
                && this.packageInfo.requestedPermissions != null
                && this.packageInfo.requestedPermissionsFlags != null) {
            // On API 31 we only need to check location here if the scan permission requests it
            for (int i = 0; i < this.packageInfo.requestedPermissions.length; i++) {
                if (Manifest.permission.BLUETOOTH_SCAN.equals(this.packageInfo.requestedPermissions[i])) {
                    if ((this.packageInfo.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_NEVER_FOR_LOCATION) != 0) {
                        // BLUETOOTH_SCAN is neverForLocation
                        checkLocationProviderState = false;
                    }
                    break;
                }
            }
        }

        if (!rxBleAdapterWrapper.hasBluetoothAdapter()) {
            throw new BleScanException(BleScanException.BLUETOOTH_NOT_AVAILABLE);
        } else if (!rxBleAdapterWrapper.isBluetoothEnabled()) {
            throw new BleScanException(BleScanException.BLUETOOTH_DISABLED);
        } else if (checkLocationProviderState && !locationServicesStatus.isLocationPermissionOk()) {
            throw new BleScanException(BleScanException.LOCATION_PERMISSION_MISSING);
        } else if (checkLocationProviderState && !locationServicesStatus.isLocationProviderOk()) {
            throw new BleScanException(BleScanException.LOCATION_SERVICES_DISABLED);
        } else if (!locationServicesStatus.isScanPermissionOk()) {
            throw new BleScanException(BleScanException.SCAN_PERMISSION_MISSING);
        }

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
