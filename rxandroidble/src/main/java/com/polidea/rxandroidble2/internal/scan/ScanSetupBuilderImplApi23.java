package com.polidea.rxandroidble2.internal.scan;


import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import com.polidea.rxandroidble2.internal.operations.ScanOperationApi21;
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanSettings;

import bleshadow.javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ScanSetupBuilderImplApi23 implements ScanSetupBuilder {

    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final InternalScanResultCreator internalScanResultCreator;
    private final AndroidScanObjectsConverter androidScanObjectsConverter;

    @Inject
    ScanSetupBuilderImplApi23(
            RxBleAdapterWrapper rxBleAdapterWrapper,
            InternalScanResultCreator internalScanResultCreator,
            AndroidScanObjectsConverter androidScanObjectsConverter
    ) {
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.internalScanResultCreator = internalScanResultCreator;
        this.androidScanObjectsConverter = androidScanObjectsConverter;
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
                        scanFilters),
                new ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult>() {
                    @Override
                    public Observable<RxBleInternalScanResult> apply(Observable<RxBleInternalScanResult> observable) {
                        return observable;
                    }
                }
        );
    }
}
