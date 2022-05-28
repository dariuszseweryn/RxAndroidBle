package com.polidea.rxandroidble2.samplekotlin.util

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.polidea.rxandroidble2.RxBleClient

private const val REQUEST_PERMISSION_BLE_CONNECT = 102

internal fun Activity.requestConnectionPermission(client: RxBleClient) =
    ActivityCompat.requestPermissions(
        this,
        /*
         * the below would cause a ArrayIndexOutOfBoundsException on API < 31. Yet it should not be called then as runtime
         * permissions are not needed and RxBleClient.isConnectRuntimePermissionGranted() returns `true`
         */
        arrayOf(client.recommendedConnectRuntimePermissions[0]),
        REQUEST_PERMISSION_BLE_CONNECT
    )

internal fun isConnectionPermissionGranted(requestCode: Int, grantResults: IntArray) =
    requestCode == REQUEST_PERMISSION_BLE_CONNECT && grantResults[0] == PackageManager.PERMISSION_GRANTED
