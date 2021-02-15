package com.polidea.rxandroidble3.internal.scan;


import androidx.annotation.RestrictTo;
import com.polidea.rxandroidble3.scan.ScanFilter;
import com.polidea.rxandroidble3.scan.ScanSettings;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ScanSetupBuilder {

    ScanSetup build(ScanSettings scanSettings, ScanFilter... scanFilters);
}
