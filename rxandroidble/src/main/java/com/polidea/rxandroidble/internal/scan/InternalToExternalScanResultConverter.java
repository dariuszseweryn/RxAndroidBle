package com.polidea.rxandroidble.internal.scan;


import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.internal.RxBleDeviceProvider;
import com.polidea.rxandroidble.scan.ScanResult;
import javax.inject.Inject;
import rx.functions.Func1;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InternalToExternalScanResultConverter implements Func1<RxBleInternalScanResult, ScanResult> {

    private final RxBleDeviceProvider deviceProvider;

    @Inject
    public InternalToExternalScanResultConverter(RxBleDeviceProvider deviceProvider) {
        this.deviceProvider = deviceProvider;
    }

    @Override
    public ScanResult call(RxBleInternalScanResult rxBleInternalScanResult) {
        return new ScanResult(
                deviceProvider.getBleDevice(rxBleInternalScanResult.getBluetoothDevice().getAddress()),
                rxBleInternalScanResult.getRssi(),
                rxBleInternalScanResult.getTimestampNanos(),
                rxBleInternalScanResult.getScanCallbackType(),
                rxBleInternalScanResult.getScanRecord()
        );
    }
}
