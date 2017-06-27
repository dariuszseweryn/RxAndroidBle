package com.polidea.rxandroidble.internal.scan;


import android.support.annotation.Nullable;
import java.util.Date;

/**
 * The interface introduced to check if app is not starting too many BLE scans in a short time frame. Android 7 (API 24) has an undocumented
 * behaviour change that it is silently (logged only) not starting a scan if too many were started. More on the topic is available here:
 * @link https://blog.classycode.com/undocumented-android-7-ble-behavior-changes-d1a9bd87d983
 * @link https://android.googlesource.com/platform/packages/apps/Bluetooth/+/master/src/com/android/bluetooth/gatt/AppScanStats.java
 */
public interface ExcessiveScanChecker {

    /**
     * Method to be called before starting the scan. Will return a suggested Date when the next check will pass
     */
    @Nullable
    Date suggestDateToRetry();
}
