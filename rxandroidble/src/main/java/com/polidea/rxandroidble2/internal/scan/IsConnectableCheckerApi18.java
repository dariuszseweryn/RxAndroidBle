package com.polidea.rxandroidble2.internal.scan;

import android.bluetooth.le.ScanResult;

import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble2.scan.IsConnectable;
import bleshadow.javax.inject.Inject;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IsConnectableCheckerApi18 implements IsConnectableChecker {

    @Inject
    public IsConnectableCheckerApi18() {
    }

    @Override
    public IsConnectable check(ScanResult scanResult) {
        return IsConnectable.LEGACY_UNKNOWN;
    }
}
