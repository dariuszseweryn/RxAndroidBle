package com.polidea.rxandroidble2.internal.scan;


import androidx.annotation.RestrictTo;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanSettings;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ScanSetupBuilder {

    List<ScanSetup> build(ScanSettings scanSettings, ScanFilter... scanFilters);
}
