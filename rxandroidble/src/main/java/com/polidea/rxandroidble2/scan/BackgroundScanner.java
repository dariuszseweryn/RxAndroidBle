package com.polidea.rxandroidble2.scan;

import android.app.PendingIntent;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;

@RequiresApi(26 /* Build.VERSION_CODES.O */)
public interface BackgroundScanner {

    /**
     * Submits a scan request that will work even if your process gets killed by the system. You can use this API to maintain a
     * background scan without a need to keep your application foreground and active. The system will manage the scan for you and
     * will wake your process up once a result is available.
     * <p>
     * When the PendingIntent is delivered, the Intent passed to the receiver or activity
     * will contain one or more of the extras {@link android.bluetooth.le.BluetoothLeScanner#EXTRA_CALLBACK_TYPE},
     * {@link android.bluetooth.le.BluetoothLeScanner#EXTRA_ERROR_CODE}
     * and {@link android.bluetooth.le.BluetoothLeScanner#EXTRA_LIST_SCAN_RESULT} to indicate the result of the scan.
     *
     * @param scanSettings   Scan settings
     * @param scanFilters    Filtering settings
     * @param callbackIntent Intent that will be executed when the scan result becomes available
     * @throws com.polidea.rxandroidble2.exceptions.BleScanException thrown if not possible to start the scan
     */
    @RequiresApi(26 /* Build.VERSION_CODES.O */)
    void scanBleDeviceInBackground(@NonNull PendingIntent callbackIntent, ScanSettings scanSettings, ScanFilter... scanFilters);

    /**
     * Stops previously initiated scan with a {@link PendingIntent} callback.
     *
     * @param callbackIntent A callback intent that was previously used to initiate a scan
     */
    @RequiresApi(26 /* Build.VERSION_CODES.O */)
    void stopBackgroundBleScan(@NonNull PendingIntent callbackIntent);

    /**
     * Callback that may be used to map received {@link Intent} from a {@link PendingIntent} based scan to a handy RxAndroidBLE enabled
     * {@link ScanResult}
     *
     * @param intent Intent containing scan result or an error code
     * @return Parsed scan result, ready to use with a RxAndroidBLE
     * @throws com.polidea.rxandroidble2.exceptions.BleScanException scan failure
     */
    List<ScanResult> onScanResultReceived(@NonNull Intent intent);
}
