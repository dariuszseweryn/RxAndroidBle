package com.polidea.rxandroidble;

import java.util.concurrent.TimeUnit;

public class TimeoutSetup {

    public final TimeUnit timeUnit;
    public final long timeout;

    public TimeoutSetup(long timeout, TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        this.timeout = timeout;
    }
}