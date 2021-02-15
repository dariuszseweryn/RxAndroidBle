package com.polidea.rxandroidble2.internal.scan;


import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble2.internal.operations.Operation;

import io.reactivex.rxjava3.core.ObservableTransformer;

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
    public final ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> scanOperationBehaviourEmulatorTransformer;

    public ScanSetup(
            Operation<RxBleInternalScanResult> scanOperation,
            ObservableTransformer<RxBleInternalScanResult, RxBleInternalScanResult> scanOperationBehaviourEmulatorTransformer
    ) {
        this.scanOperation = scanOperation;
        this.scanOperationBehaviourEmulatorTransformer = scanOperationBehaviourEmulatorTransformer;
    }
}
