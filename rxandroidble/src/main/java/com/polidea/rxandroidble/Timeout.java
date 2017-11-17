package com.polidea.rxandroidble;

import java.util.concurrent.TimeUnit;

public class Timeout {

    public final TimeUnit timeUnit;
    public final long timeout;

    public Timeout(long timeout, TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        this.timeout = timeout;
    }
}
