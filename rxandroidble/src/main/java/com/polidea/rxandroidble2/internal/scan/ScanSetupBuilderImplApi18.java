package com.polidea.rxandroidble2.internal.scan;


import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble2.internal.operations.ScanOperationApi18;
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanSettings;

import bleshadow.javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import java.util.ArrayList;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ScanSetupBuilderImplApi18 implements ScanSetupBuilder {

    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final InternalScanResultCreator internalScanResultCreator;
    private final ScanSettingsEmulator scanSettingsEmulator;

    @Inject
    ScanSetupBuilderImplApi18(
            RxBleAdapterWrapper rxBleAdapterWrapper,
            InternalScanResultCreator internalScanResultCreator,
            ScanSettingsEmulator scanSettingsEmulator
    ) {
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.internalScanResultCreator = internalScanResultCreator;
        this.scanSettingsEmulator = scanSettingsEmulator;
    }

    @Override
    public List<ScanSetup> build(ScanSettings scanSettings, ScanFilter... scanFilters) {
        final ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> scanModeTransformer
                = scanSettingsEmulator.emulateScanMode(scanSettings.getScanMode());
        final ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> callbackTypeTransformer
                = scanSettingsEmulator.emulateCallbackType(scanSettings.getCallbackType());
        ArrayList<ScanSetup> scanSetups = new ArrayList<>(1);
        scanSetups.add(new ScanSetup(
                new ScanOperationApi18(
                        rxBleAdapterWrapper,
                        internalScanResultCreator,
                        new EmulatedScanFilterMatcher(scanFilters)
                ),
                new ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult>() {
                    @Override
                    public Observable<RxBleInternalScanResult> apply(Observable<RxBleInternalScanResult> observable) {
                        return observable.compose(scanModeTransformer)
                                .compose(callbackTypeTransformer);
                    }
                }
        ));
        return scanSetups;
    }
}
