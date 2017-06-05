package com.polidea.rxandroidble.internal.scan;


import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.internal.operations.Operation;
import rx.Observable;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ScanSetup {

    /**
     * The scan operation for the device API level
     */
    public final Operation<RxBleInternalScanResult> scanOperation;
    /**
     * Some functionality (behaviour) is not supported by hardware on older APIs. scanOperationBehaviourEmulatorTransformer is returned
     * by {@link ScanSetupBuilder} from combined emulation transformers provided by {@link ScanSettingsEmulator}
     */
    public final Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> scanOperationBehaviourEmulatorTransformer;

    public ScanSetup(
            Operation<RxBleInternalScanResult> scanOperation,
            Observable.Transformer<RxBleInternalScanResult, RxBleInternalScanResult> scanOperationBehaviourEmulatorTransformer
    ) {
        this.scanOperation = scanOperation;
        this.scanOperationBehaviourEmulatorTransformer = scanOperationBehaviourEmulatorTransformer;
    }
}
