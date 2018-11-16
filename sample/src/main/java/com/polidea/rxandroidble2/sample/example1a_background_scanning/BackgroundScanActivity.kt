package com.polidea.rxandroidble2.sample.example1a_background_scanning

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import butterknife.ButterKnife
import butterknife.OnClick
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.sample.R
import com.polidea.rxandroidble2.sample.SampleApplication
import com.polidea.rxandroidble2.sample.util.checkLocationPermissionGranted
import com.polidea.rxandroidble2.sample.util.handleException
import com.polidea.rxandroidble2.sample.util.isRequestLocationPermissionGranted
import com.polidea.rxandroidble2.sample.util.requestLocationPermission
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings

private const val SCAN_REQUEST_CODE = 42

internal fun Context.newBackgroundScanActivity(): Intent = Intent(this, BackgroundScanActivity::class.java)

class BackgroundScanActivity : AppCompatActivity() {

    private val rxBleClient = SampleApplication.rxBleClient

    private val callbackIntent = newScanReceiverPendingIntent(SCAN_REQUEST_CODE)

    private var hasClickedScan = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example1a)
        ButterKnife.bind(this)
    }

    @OnClick(R.id.scan_start_btn)
    fun onScanStartClick() {
        if (checkLocationPermissionGranted()) {
            scanBleDeviceInBackground()
        } else {
            hasClickedScan = true
            requestLocationPermission()
        }
    }

    private fun scanBleDeviceInBackground() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .build()

                val scanFilter = ScanFilter.Builder()
//                    .setDeviceAddress("5C:31:3E:BF:F7:34")
                    // add custom filters if needed
                    .build()

                rxBleClient.backgroundScanner.scanBleDeviceInBackground(callbackIntent, scanSettings, scanFilter)
            } else {
                Toast.makeText(this, "Background scanning requires at least API 26", Toast.LENGTH_SHORT).show()
            }
        } catch (scanException: BleScanException) {
            Log.w("BackgroundScanActivity", "Failed to start background scan", scanException)
            handleException(scanException)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (isRequestLocationPermissionGranted(requestCode, permissions, grantResults) && hasClickedScan) {
            hasClickedScan = false
            scanBleDeviceInBackground()
        }
    }

    @OnClick(R.id.scan_stop_btn)
    fun onScanStopClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            rxBleClient.backgroundScanner.stopBackgroundBleScan(callbackIntent)
        } else {
            Toast.makeText(this, "Background scanning requires at least API 26", Toast.LENGTH_SHORT).show()
        }
    }
}
