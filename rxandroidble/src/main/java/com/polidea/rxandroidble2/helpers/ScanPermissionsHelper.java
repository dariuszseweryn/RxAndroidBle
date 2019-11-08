package com.polidea.rxandroidble2.helpers;

public interface ScanPermissionsHelper {

    /**
     * Returns whether runtime permissions needed to run a BLE scan are granted. If permissions are not granted then one may check
     * {@link #getRecommendedScanRuntimePermissions()} to get Android runtime permission strings needed for running a scan.
     *
     * @return true if needed permissions are granted, false otherwise
     */
    boolean isScanRuntimePermissionGranted();

    /**
     * Returns permission strings needed by the application to run a BLE scan or an empty array if no runtime permissions are needed. Since
     * Android 6.0 runtime permissions were introduced. To run a BLE scan a runtime permission is needed ever since. Since Android 10.0
     * a different (finer) permission is needed. Only a single permission returned by this function is needed to perform a scan. It is up
     * to the user to decide which one. The result array is sorted with the least permissive values first.
     * <p>
     * Returned values:
     * <p>
     * case: API < 23<p>
     * Empty array. No runtime permissions needed.
     * <p>
     * case: 23 <= API < 29<p>
     * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}
     * <p>
     * case: 29 <= API<p>
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}
     *
     * @return an ordered array of possible scan permissions
     */
    String[] getRecommendedScanRuntimePermissions();
}
