package com.polidea.rxandroidble2.internal.scan;

import android.bluetooth.le.ScanResult;

import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AdvertisingSidExtractor {
    Integer extract(ScanResult scanResult);
}
