package com.polidea.rxandroidble.exceptions;

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
}
