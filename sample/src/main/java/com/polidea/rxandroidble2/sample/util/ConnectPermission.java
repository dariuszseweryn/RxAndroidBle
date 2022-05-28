package com.polidea.rxandroidble2.sample.util;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import com.polidea.rxandroidble2.RxBleClient;

public class ConnectPermission {

    private ConnectPermission() {
        // Utility class
    }

    private static final int REQUEST_PERMISSION_BLE_CONNECT = 9359;

    public static void requestConnectionPermission(final Activity activity, final RxBleClient client) {
        ActivityCompat.requestPermissions(
                activity,
                /*
                 * the below would cause a ArrayIndexOutOfBoundsException on API < 31. Yet it should not be called then as runtime
                 * permissions are not needed and RxBleClient.isConnectRuntimePermissionGranted() returns `true`
                 */
                new String[]{client.getRecommendedConnectRuntimePermissions()[0]},
                REQUEST_PERMISSION_BLE_CONNECT
        );
    }

    public static boolean isRequestConnectionPermissionGranted(final int requestCode, final String[] permissions,
                                                               final int[] grantResults, RxBleClient client) {
        if (requestCode != REQUEST_PERMISSION_BLE_CONNECT) {
            return false;
        }

        String[] recommendedConnectRuntimePermissions = client.getRecommendedConnectRuntimePermissions();
        for (int i = 0; i < permissions.length; i++) {
            for (String recommendedScanRuntimePermission : recommendedConnectRuntimePermissions) {
                if (permissions[i].equals(recommendedScanRuntimePermission)
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
        }

        return false;
    }
}
