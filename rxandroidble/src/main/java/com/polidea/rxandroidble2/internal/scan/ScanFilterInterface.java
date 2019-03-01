package com.polidea.rxandroidble2.internal.scan;

public interface ScanFilterInterface {

    boolean isAllFieldsEmpty();

    boolean matches(RxBleInternalScanResult scanResult);
}
