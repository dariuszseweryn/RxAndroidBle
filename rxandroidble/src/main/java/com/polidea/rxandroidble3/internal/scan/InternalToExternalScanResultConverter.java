package com.polidea.rxandroidble2.internal.scan;


import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble2.internal.RxBleDeviceProvider;
import com.polidea.rxandroidble2.scan.ScanResult;

import bleshadow.javax.inject.Inject;

import io.reactivex.rxjava3.functions.Function;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InternalToExternalScanResultConverter implements Function<RxBleInternalScanResult, ScanResult> {

    private final RxBleDeviceProvider deviceProvider;

    @Inject
    public InternalToExternalScanResultConverter(RxBleDeviceProvider deviceProvider) {
        this.deviceProvider = deviceProvider;
    }

    @Override
    public ScanResult apply(RxBleInternalScanResult rxBleInternalScanResult) {
        return new ScanResult(
                deviceProvider.getBleDevice(rxBleInternalScanResult.getBluetoothDevice().getAddress()),
                rxBleInternalScanResult.getRssi(),
                rxBleInternalScanResult.getTimestampNanos(),
                rxBleInternalScanResult.getScanCallbackType(),
                rxBleInternalScanResult.getScanRecord()
        );
    }
}
