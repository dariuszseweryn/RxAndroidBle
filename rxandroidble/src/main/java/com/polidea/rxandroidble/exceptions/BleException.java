package com.polidea.rxandroidble.exceptions;

import android.support.annotation.Nullable;

public class BleException extends RuntimeException {

    public BleException() {
        super();
    }

    public BleException(String message) {
        super(message);
    }

    public BleException(Throwable throwable) {
        super(throwable);
    }

    static String toStringCauseIfExists(@Nullable Throwable throwableCause) {
        return (throwableCause != null ? ", cause=" + throwableCause.toString() : "");
    }
}
