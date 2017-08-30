package com.polidea.rxandroidble.internal.scan;


import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.internal.operations.ScanOperationApi21;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import com.polidea.rxandroidble.scan.ScanFilter;
import com.polidea.rxandroidble.scan.ScanSettings;
import javax.inject.Inject;
import rx.Observable;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ScanSetupBuilderImplApi21 implements ScanSetupBuilder {

    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final InternalScanResultCreator internalScanResultCreator;
    private final ScanSettingsEmulator scanSettingsEmulator;
    private final AndroidScanObjectsConverter androidScanObjectsConverter;

    @Inject
    ScanSetupBuilderImplApi21(
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public ScanSetup build(ScanSettings scanSettings, ScanFilter... scanFilters) {
        /*
         Android 5.0 (API21) does not handle FIRST_MATCH and / or MATCH_LOST callback type
         https://developer.android.com/reference/android/bluetooth/le/ScanSettings.Builder.html#setCallbackType(int)
          */
        final Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> callbackTypeTransformer
                = scanSettingsEmulator.emulateCallbackType(scanSettings.getCallbackType());
        return new ScanSetup(
                new ScanOperationApi21(
                        rxBleAdapterWrapper,
                        internalScanResultCreator,
                        androidScanObjectsConverter,
                        scanSettings,
                        new EmulatedScanFilterMatcher(scanFilters),
                        null),
                new Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult>() {
                    @Override
                    public Observable<RxBleInternalScanResult> call(Observable<RxBleInternalScanResult> observable) {
                        return observable.compose(callbackTypeTransformer);
                    }
                }
        );
    }
}
