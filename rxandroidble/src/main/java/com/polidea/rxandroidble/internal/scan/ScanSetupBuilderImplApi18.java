package com.polidea.rxandroidble.internal.scan;


import android.support.annotation.RestrictTo;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.internal.eventlog.OperationEventLogger;
import com.polidea.rxandroidble.internal.operations.ScanOperationApi18;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanSettings;

import bleshadow.javax.inject.Named;
import bleshadow.javax.inject.Inject;
import rx.Observable;
import rx.Scheduler;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ScanSetupBuilderImplApi18 implements ScanSetupBuilder {

    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final InternalScanResultCreator internalScanResultCreator;
    private final ScanSettingsEmulator scanSettingsEmulator;
    private final OperationEventLogger eventLogger;
    private final Scheduler callbackScheduler;

    @Inject
    ScanSetupBuilderImplApi18(
            RxBleAdapterWrapper rxBleAdapterWrapper,
            InternalScanResultCreator internalScanResultCreator,
            ScanSettingsEmulator scanSettingsEmulator,
            OperationEventLogger eventLogger,
            @Named(ClientComponent.NamedSchedulers.BLUETOOTH_CALLBACKS) Scheduler callbackScheduler
    ) {
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.internalScanResultCreator = internalScanResultCreator;
        this.scanSettingsEmulator = scanSettingsEmulator;
        this.eventLogger = eventLogger;
        this.callbackScheduler = callbackScheduler;
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
                        new EmulatedScanFilterMatcher(scanFilters),
                        eventLogger,
                        callbackScheduler
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
