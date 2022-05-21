package com.polidea.rxandroidble2.internal.scan;

import android.bluetooth.le.ScanResult;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble2.scan.IsConnectable;
import bleshadow.javax.inject.Inject;

@RequiresApi(26 /* Build.VERSION_CODES.O */)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IsConnectableCheckerApi26 implements IsConnectableChecker {

    @Inject
    public IsConnectableCheckerApi26() {
    }

    @Override
    public IsConnectable check(ScanResult scanResult) {
        return scanResult.isConnectable() ? IsConnectable.CONNECTABLE : IsConnectable.NOT_CONNECTABLE;
    }
}
