package com.polidea.rxandroidble3.internal.scan;

import com.polidea.rxandroidble3.internal.ScanResultInterface;

public interface ScanFilterInterface {

    boolean isAllFieldsEmpty();

    boolean matches(ScanResultInterface scanResult);
}
