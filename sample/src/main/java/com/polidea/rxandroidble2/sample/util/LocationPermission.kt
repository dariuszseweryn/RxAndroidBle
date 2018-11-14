package com.polidea.rxandroidble2.sample.util

import android.Manifest
import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

object LocationPermission {

    private val REQUEST_PERMISSION_COARSE_LOCATION = 9358

    fun checkLocationPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission.ACCESS_COARSE_LOCATION),
            REQUEST_PERMISSION_COARSE_LOCATION
        )
    }

    fun isRequestLocationPermissionGranted(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode == REQUEST_PERMISSION_COARSE_LOCATION) {
            for (i in permissions.indices) {
                if (permissions[i] == Manifest.permission.ACCESS_COARSE_LOCATION && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    return true
                }
            }
        }
        return false
    }
}// Utility class
