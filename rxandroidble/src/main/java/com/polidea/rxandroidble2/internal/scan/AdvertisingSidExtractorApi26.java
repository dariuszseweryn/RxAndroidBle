package com.polidea.rxandroidble2.internal.scan;

import android.bluetooth.le.ScanResult;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import bleshadow.javax.inject.Inject;

@RequiresApi(26 /* Build.VERSION_CODES.O */)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AdvertisingSidExtractorApi26 implements AdvertisingSidExtractor {
    @Inject
    public AdvertisingSidExtractorApi26() {
    }

    @Override
    public Integer extract(ScanResult scanResult) {
        return scanResult.getAdvertisingSid();
    }
}
