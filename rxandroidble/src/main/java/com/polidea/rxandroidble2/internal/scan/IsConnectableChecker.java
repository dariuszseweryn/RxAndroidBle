package com.polidea.rxandroidble2.internal.scan;

import android.bluetooth.le.ScanResult;

import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble2.scan.IsConnectable;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface IsConnectableChecker {
    IsConnectable check(ScanResult scanResult);
}
