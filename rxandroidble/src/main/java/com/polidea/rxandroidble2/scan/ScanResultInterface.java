package com.polidea.rxandroidble2.scan;

public interface ScanResultInterface {
    String getAddress();

    int getRssi();

    ScanRecord getScanRecord();

    long getTimestampNanos();

    ScanCallbackType getScanCallbackType();
}
