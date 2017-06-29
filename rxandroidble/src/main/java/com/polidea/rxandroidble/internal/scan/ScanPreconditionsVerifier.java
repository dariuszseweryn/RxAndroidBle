package com.polidea.rxandroidble.internal.scan;


import com.polidea.rxandroidble.exceptions.BleScanException;

public interface ScanPreconditionsVerifier {

    void verify() throws BleScanException;
}
