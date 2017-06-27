package com.polidea.rxandroidble.internal.scan;


import android.support.annotation.Nullable;
import java.util.Date;

public class ExcessiveScanCheckerApi18 implements ExcessiveScanChecker {

    @Nullable
    @Override
    public Date suggestDateToRetry() {
        return null; // not needed prior to API 24
    }
}
