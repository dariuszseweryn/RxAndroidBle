package com.polidea.rxandroidble3.internal.scan;


import androidx.annotation.Nullable;
import java.util.Arrays;

public class EmulatedScanFilterMatcher {

    @Nullable
    private final ScanFilterInterface[] scanFilters;
    private final boolean isEmpty;

    public EmulatedScanFilterMatcher(@Nullable ScanFilterInterface... scanFilters) {
        this.scanFilters = scanFilters;
        boolean tempIsEmpty = true;
        if (scanFilters != null && scanFilters.length != 0) {
            for (ScanFilterInterface scanFilter : scanFilters) {
                if (!scanFilter.isAllFieldsEmpty()) {
                    tempIsEmpty = false;
                    break;
                }
            }
        }
        isEmpty = tempIsEmpty;
    }

    public boolean matches(RxBleInternalScanResult internalScanResult) {
        if (scanFilters == null || scanFilters.length == 0) {
            return true;
        }

        for (ScanFilterInterface scanFilter : scanFilters) {
            if (scanFilter.matches(internalScanResult)) {
                return true;
            }
        }

        return false;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    @Override
    public String toString() {
        return "emulatedFilters=" + Arrays.toString(scanFilters);
    }
}
