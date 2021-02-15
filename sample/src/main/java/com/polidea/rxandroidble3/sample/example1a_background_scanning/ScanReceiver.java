package com.polidea.rxandroidble3.sample.example1a_background_scanning;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.RequiresApi;
import android.util.Log;

import com.polidea.rxandroidble3.exceptions.BleScanException;
import com.polidea.rxandroidble3.sample.SampleApplication;
import com.polidea.rxandroidble3.scan.BackgroundScanner;
import com.polidea.rxandroidble3.scan.ScanResult;

import java.util.List;

public class ScanReceiver extends BroadcastReceiver {

    @RequiresApi(26 /* Build.VERSION_CODES.O */)
    @Override
    public void onReceive(Context context, Intent intent) {
        BackgroundScanner backgroundScanner = SampleApplication.getRxBleClient(context).getBackgroundScanner();

        try {
            final List<ScanResult> scanResults = backgroundScanner.onScanResultReceived(intent);
            Log.i("ScanReceiver", "Scan results received: " + scanResults);
        } catch (BleScanException exception) {
            Log.w("ScanReceiver", "Failed to scan devices", exception);
        }
    }
}
