package com.polidea.rxandroidble.internal.scan;


import android.support.annotation.Nullable;
import com.polidea.rxandroidble.scan.ScanFilter;

public class EmulatedScanFilterMatcher {

    @Nullable
    private final ScanFilter[] scanFilters;

    public EmulatedScanFilterMatcher(@Nullable ScanFilter... scanFilters) {
        this.scanFilters = scanFilters;
    }

    public boolean matches(RxBleInternalScanResult internalScanResult) {
        if (scanFilters == null || scanFilters.length == 0) {
            return true;
        }

        for (ScanFilter scanFilter : scanFilters) {
            if (scanFilter.matches(internalScanResult)) {
                return true;
            }
        }

        return false;
    }
}
