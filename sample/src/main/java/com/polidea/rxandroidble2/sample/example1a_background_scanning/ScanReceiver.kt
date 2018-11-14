package com.polidea.rxandroidble2.sample.example1a_background_scanning

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log

import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.sample.SampleApplication
import com.polidea.rxandroidble2.scan.BackgroundScanner
import com.polidea.rxandroidble2.scan.ScanResult

class ScanReceiver : BroadcastReceiver() {

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val backgroundScanner = SampleApplication.getRxBleClient(context)!!.backgroundScanner

        try {
            val scanResults = backgroundScanner.onScanResultReceived(intent)
            Log.i("ScanReceiver", "Scan results received: $scanResults")
        } catch (exception: BleScanException) {
            Log.w("ScanReceiver", "Failed to scan devices", exception)
        }
    }
}
