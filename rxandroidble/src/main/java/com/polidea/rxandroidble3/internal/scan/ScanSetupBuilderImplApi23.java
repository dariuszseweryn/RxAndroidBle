package com.polidea.rxandroidble3.internal.scan;


import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble3.internal.RxBleLog;
import com.polidea.rxandroidble3.internal.operations.ScanOperationApi21;
import com.polidea.rxandroidble3.internal.util.ObservableUtil;
import com.polidea.rxandroidble3.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble3.scan.ScanFilter;
import com.polidea.rxandroidble3.scan.ScanSettings;

import bleshadow.javax.inject.Inject;

import io.reactivex.rxjava3.core.ObservableTransformer;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ScanSetupBuilderImplApi23 implements ScanSetupBuilder {

    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final InternalScanResultCreator internalScanResultCreator;
    private final ScanSettingsEmulator scanSettingsEmulator;
    private final AndroidScanObjectsConverter androidScanObjectsConverter;

    @Inject
    ScanSetupBuilderImplApi23(
            RxBleAdapterWrapper rxBleAdapterWrapper,
            InternalScanResultCreator internalScanResultCreator,
            ScanSettingsEmulator scanSettingsEmulator,
            AndroidScanObjectsConverter androidScanObjectsConverter
    ) {
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.internalScanResultCreator = internalScanResultCreator;
        this.scanSettingsEmulator = scanSettingsEmulator;
        this.androidScanObjectsConverter = androidScanObjectsConverter;
    }

    @RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
    @Override
    public ScanSetup build(ScanSettings scanSettings, ScanFilter... scanFilters) {
        boolean areFiltersSpecified = areFiltersSpecified(scanFilters);
        boolean isFilteringCallbackType = scanSettings.getCallbackType() != ScanSettings.CALLBACK_TYPE_ALL_MATCHES;

        ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> resultTransformer = ObservableUtil.identityTransformer();
        ScanSettings resultScanSettings = scanSettings;

        // native matching (when a device is first seen or no longer seen) does not work with no filters specified —
        // see https://issuetracker.google.com/issues/37127640
        // so we will use a callback type that will work and emulate the desired behaviour
        boolean shouldEmulateCallbackType = isFilteringCallbackType && !areFiltersSpecified;
        if (shouldEmulateCallbackType) {
            RxBleLog.d("ScanSettings.callbackType != CALLBACK_TYPE_ALL_MATCHES but no (or only empty) filters are specified. "
                + "Falling back to callbackType emulation.");
            resultTransformer = scanSettingsEmulator.emulateCallbackType(scanSettings.getCallbackType());
            resultScanSettings = scanSettings.copyWithCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        }

        return new ScanSetup(
                new ScanOperationApi21(
                        rxBleAdapterWrapper,
                        internalScanResultCreator,
                        androidScanObjectsConverter,
                        resultScanSettings,
                        new EmulatedScanFilterMatcher(),
                        scanFilters),
                resultTransformer
        );
    }

    private static boolean areFiltersSpecified(ScanFilter[] scanFilters) {
        boolean scanFiltersEmpty = true;
        for (ScanFilter scanFilter : scanFilters) {
            scanFiltersEmpty &= scanFilter.isAllFieldsEmpty();
        }
        return !scanFiltersEmpty;
    }
}
