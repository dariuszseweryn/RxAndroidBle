package com.polidea.rxandroidble2.internal.scan;


import static com.polidea.rxandroidble2.internal.scan.ExternalScanSettingsExtension.API2_EMULATED_FILTERS;
import static com.polidea.rxandroidble2.internal.scan.ExternalScanSettingsExtension.API2_EMULATED_SETTINGS;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble2.internal.operations.ScanOperationApi21;
import com.polidea.rxandroidble2.internal.util.ObservableUtil;
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanSettings;

import bleshadow.javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import java.util.ArrayList;
import java.util.List;

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
    public List<ScanSetup> build(ScanSettings scanSettings, ScanFilter... scanFilters) {
        // for now assuming that on Android 6.0+ there are no problems
        scanFilters[0].isAllFieldsEmpty();

        if (scanSettings.getCallbackType() != ScanSettings.CALLBACK_TYPE_ALL_MATCHES && scanFilters.length == 0) {
            // native matching does not work with no filters specified - see https://issuetracker.google.com/issues/37127640
            scanFilters = new ScanFilter[] {
                    ScanFilter.empty()
            };
        }
        ArrayList<ScanSetup> scanSetups = new ArrayList<>();
        for (int emulationFlag : scanSettings.getEmulationFlags()) {
            final boolean emulateSettings = (emulationFlag & API2_EMULATED_SETTINGS) != 0;
            final ScanSettings resultScanSettings = emulateSettings
                    ? new ScanSettings.Builder()
                    .setScanMode(scanSettings.getScanMode())
                    .setShouldCheckLocationServicesState(scanSettings.shouldCheckLocationProviderState())
                    .build()
                    : scanSettings;
            ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> resultTransformer = emulateSettings
                    ? scanSettingsEmulator.emulateCallbackType(scanSettings.getCallbackType())
                    : ObservableUtil.<RxBleInternalScanResult>identityTransformer();

            boolean emulateFilters = (emulationFlag & API2_EMULATED_FILTERS) != 0;
            EmulatedScanFilterMatcher resultEmulatedScanFilterMatcher = emulateFilters
                    ? new EmulatedScanFilterMatcher(scanFilters)
                    : new EmulatedScanFilterMatcher();
            ScanFilter[] resultScanFilters = emulateFilters
                    ? null
                    : scanFilters;

            scanSetups.add(new ScanSetup(
                    new ScanOperationApi21(
                            rxBleAdapterWrapper,
                            internalScanResultCreator,
                            androidScanObjectsConverter,
                            resultScanSettings,
                            resultEmulatedScanFilterMatcher,
                            resultScanFilters),
                    resultTransformer
            ));
        }
        return scanSetups;
    }
}
