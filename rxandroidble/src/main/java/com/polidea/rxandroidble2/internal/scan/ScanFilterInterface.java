package com.polidea.rxandroidble2.internal.scan;

import com.polidea.rxandroidble2.internal.ScanResultInterface;

public interface ScanFilterInterface {

    boolean isAllFieldsEmpty();

    boolean matches(ScanResultInterface scanResult);
}
