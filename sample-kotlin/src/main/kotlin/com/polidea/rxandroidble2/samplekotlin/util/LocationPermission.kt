package com.polidea.rxandroidble2.samplekotlin.util

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private const val REQUEST_PERMISSION_COARSE_LOCATION = 101

internal fun Context.checkLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

internal fun Activity.requestLocationPermission() =
    ActivityCompat.requestPermissions(
        this,
        arrayOf(permission.ACCESS_COARSE_LOCATION),
        REQUEST_PERMISSION_COARSE_LOCATION
    )

internal fun isLocationPermissionGranted(requestCode: Int, grantResults: IntArray) =
    requestCode == REQUEST_PERMISSION_COARSE_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED
