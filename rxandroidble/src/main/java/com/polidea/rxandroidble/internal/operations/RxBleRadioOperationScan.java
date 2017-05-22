package com.polidea.rxandroidble.internal.operations;


import com.polidea.rxandroidble.internal.scan.RxBleInternalScanResult;
import com.polidea.rxandroidble.internal.RxBleRadioOperationReusable;

abstract public class RxBleRadioOperationScan extends RxBleRadioOperationReusable<RxBleInternalScanResult> {

    abstract public void stop();
}
