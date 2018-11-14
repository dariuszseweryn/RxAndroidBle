package com.polidea.rxandroidble2.sample.example1a_background_scanning

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log

import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.sample.R
import com.polidea.rxandroidble2.sample.SampleApplication
import com.polidea.rxandroidble2.sample.util.ScanExceptionHandler
import com.polidea.rxandroidble2.sample.util.LocationPermission
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanSettings

import butterknife.ButterKnife
import butterknife.OnClick

class BackgroundScanActivity : AppCompatActivity() {
    private val rxBleClient = SampleApplication.rxBleClient
    private var callbackIntent: PendingIntent? = null
    private var hasClickedScan: Boolean = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_example1a)
        ButterKnife.bind(this)
        callbackIntent = PendingIntent.getBroadcast(
            this, SCAN_REQUEST_CODE,
            Intent(this, ScanReceiver::class.java), 0
        )
    }

    @OnClick(R.id.scan_start_btn)
    fun onScanStartClick() {
        hasClickedScan = true
        if (LocationPermission.checkLocationPermissionGranted(this)) {
            scanBleDeviceInBackground()
        } else {
            LocationPermission.requestLocationPermission(this)
        }
    }

    private fun scanBleDeviceInBackground() {
        try {
            rxBleClient.backgroundScanner.scanBleDeviceInBackground(
                callbackIntent!!,
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .build(),
                ScanFilter.Builder()
                    .setDeviceAddress("5C:31:3E:BF:F7:34")
                    // add custom filters if needed
                    .build()
            )
        } catch (scanException: BleScanException) {
            Log.w("BackgroundScanActivity", "Failed to start background scan", scanException)
            ScanExceptionHandler.handleException(this, scanException)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (LocationPermission.isRequestLocationPermissionGranted(
                requestCode,
                permissions,
                grantResults
            ) && hasClickedScan
        ) {
            hasClickedScan = false
            scanBleDeviceInBackground()
        }
    }

    @OnClick(R.id.scan_stop_btn)
    fun onScanStopClick() {
        rxBleClient.backgroundScanner.stopBackgroundBleScan(callbackIntent!!)
    }

    companion object {

        private val SCAN_REQUEST_CODE = 42
    }
}
