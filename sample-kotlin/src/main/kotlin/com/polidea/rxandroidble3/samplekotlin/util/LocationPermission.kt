package com.polidea.rxandroidble2.samplekotlin.util

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.polidea.rxandroidble2.RxBleClient

private const val REQUEST_PERMISSION_BLE_SCAN = 101

internal fun Activity.requestLocationPermission(client: RxBleClient) =
    ActivityCompat.requestPermissions(
        this,
        /*
         * the below would cause a ArrayIndexOutOfBoundsException on API < 23. Yet it should not be called then as runtime
         * permissions are not needed and RxBleClient.isScanRuntimePermissionGranted() returns `true`
         */
        arrayOf(client.recommendedScanRuntimePermissions[0]),
        REQUEST_PERMISSION_BLE_SCAN
    )

internal fun isLocationPermissionGranted(requestCode: Int, grantResults: IntArray) =
    requestCode == REQUEST_PERMISSION_BLE_SCAN && grantResults[0] == PackageManager.PERMISSION_GRANTED
