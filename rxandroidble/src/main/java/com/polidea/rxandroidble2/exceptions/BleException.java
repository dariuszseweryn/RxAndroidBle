package com.polidea.rxandroidble2.exceptions;

/**
 * Base class of exceptions in this project.
 */
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

    public BleException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
