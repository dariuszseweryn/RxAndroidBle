package com.polidea.rxandroidble.internal.scan;


import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.internal.operations.ScanOperationApi18;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanSettings;
import javax.inject.Inject;
import rx.Observable;

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
    public ScanSetup build(ScanSettings scanSettings, ScanFilter... scanFilters) {
        final Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> scanModeTransformer
                = scanSettingsEmulator.emulateScanMode(scanSettings.getScanMode());
        final Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> callbackTypeTransformer
                = scanSettingsEmulator.emulateCallbackType(scanSettings.getCallbackType());
        return new ScanSetup(
                new ScanOperationApi18(
                        rxBleAdapterWrapper,
                        internalScanResultCreator,
                        new EmulatedScanFilterMatcher(scanFilters)
                ),
                new Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult>() {
                    @Override
                    public Observable<RxBleInternalScanResult> call(Observable<RxBleInternalScanResult> observable) {
                        return observable.compose(scanModeTransformer)
                                .compose(callbackTypeTransformer);
                    }
                }
        );
    }
}
