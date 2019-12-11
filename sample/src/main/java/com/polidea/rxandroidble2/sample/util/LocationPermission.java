package com.polidea.rxandroidble2.sample.util;

import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import com.polidea.rxandroidble2.RxBleClient;

public class LocationPermission {

    private LocationPermission() {
        // Utility class
    }

    private static final int REQUEST_PERMISSION_BLE_SCAN = 9358;

    public static void requestLocationPermission(final Activity activity, final RxBleClient client) {
        ActivityCompat.requestPermissions(
                activity,
                /*
                 * the below would cause a ArrayIndexOutOfBoundsException on API < 23. Yet it should not be called then as runtime
                 * permissions are not needed and RxBleClient.isScanRuntimePermissionGranted() returns `true`
                 */
                new String[]{client.getRecommendedScanRuntimePermissions()[0]},
                REQUEST_PERMISSION_BLE_SCAN
        );
    }

    public static boolean isRequestLocationPermissionGranted(final int requestCode, final String[] permissions,
                                                             final int[] grantResults, RxBleClient client) {
        if (requestCode != REQUEST_PERMISSION_BLE_SCAN) {
            return false;
        }

        String[] recommendedScanRuntimePermissions = client.getRecommendedScanRuntimePermissions();
        for (int i = 0; i < permissions.length; i++) {
            for (String recommendedScanRuntimePermission : recommendedScanRuntimePermissions) {
                if (permissions[i].equals(recommendedScanRuntimePermission)
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
        }

        return false;
    }
}
