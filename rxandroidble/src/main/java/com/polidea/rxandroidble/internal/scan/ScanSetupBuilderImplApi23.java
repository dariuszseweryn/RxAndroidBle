package com.polidea.rxandroidble.internal.scan;


import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.eventlog.OperationEventLogger;
import com.polidea.rxandroidble.internal.operations.ScanOperationApi21;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanSettings;

import bleshadow.javax.inject.Named;
import bleshadow.javax.inject.Inject;
import rx.Observable;
import rx.Scheduler;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ScanSetupBuilderImplApi23 implements ScanSetupBuilder {

    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final InternalScanResultCreator internalScanResultCreator;
    private final AndroidScanObjectsConverter androidScanObjectsConverter;
    private final OperationEventLogger eventLogger;
    private final Scheduler callbackScheduler;

    @Inject
    ScanSetupBuilderImplApi23(
            RxBleAdapterWrapper rxBleAdapterWrapper,
            InternalScanResultCreator internalScanResultCreator,
            AndroidScanObjectsConverter androidScanObjectsConverter,
            OperationEventLogger eventLogger,
            @Named(ClientComponent.NamedSchedulers.BLUETOOTH_CALLBACKS) Scheduler callbackScheduler
    ) {
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.internalScanResultCreator = internalScanResultCreator;
        this.androidScanObjectsConverter = androidScanObjectsConverter;
        this.eventLogger = eventLogger;
        this.callbackScheduler = callbackScheduler;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public ScanSetup build(ScanSettings scanSettings, ScanFilter... scanFilters) {
        // for now assuming that on Android 6.0+ there are no problems

        if (scanSettings.getCallbackType() != ScanSettings.CALLBACK_TYPE_ALL_MATCHES && scanFilters.length == 0) {
            // native matching does not work with no filters specified - see https://issuetracker.google.com/issues/37127640
            scanFilters = new ScanFilter[] {
                    ScanFilter.empty()
            };
        }
        return new ScanSetup(
                new ScanOperationApi21(
                        rxBleAdapterWrapper,
                        internalScanResultCreator,
                        androidScanObjectsConverter,
                        scanSettings,
                        new EmulatedScanFilterMatcher(),
                        scanFilters,
                        eventLogger,
                        callbackScheduler),
                new Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult>() {
                    @Override
                    public Observable<RxBleInternalScanResult> call(Observable<RxBleInternalScanResult> observable) {
                        return observable;
                    }
                }
        );
    }
}
