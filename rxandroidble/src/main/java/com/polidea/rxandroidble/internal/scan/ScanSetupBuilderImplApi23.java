package com.polidea.rxandroidble.internal.scan;


import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationScanApi21;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanSettings;
import javax.inject.Inject;
import rx.Observable;

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
                new RxBleRadioOperationScanApi21(
                        rxBleAdapterWrapper,
                        internalScanResultCreator,
                        androidScanObjectsConverter,
                        scanSettings,
                        new EmulatedScanFilterMatcher(),
                        scanFilters),
                new Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult>() {
                    @Override
                    public Observable<RxBleInternalScanResult> call(Observable<RxBleInternalScanResult> observable) {
                        return observable;
                    }
                }
        );
    }
}
