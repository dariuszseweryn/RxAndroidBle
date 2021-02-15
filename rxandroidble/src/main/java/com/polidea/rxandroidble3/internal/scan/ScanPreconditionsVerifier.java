package com.polidea.rxandroidble3.internal.scan;


import com.polidea.rxandroidble3.exceptions.BleScanException;

public interface ScanPreconditionsVerifier {

    void verify(boolean checkLocationProviderState) throws BleScanException;
}
