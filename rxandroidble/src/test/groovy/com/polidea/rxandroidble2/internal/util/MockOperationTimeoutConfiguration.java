package com.polidea.rxandroidble2.internal.util;

import com.polidea.rxandroidble2.internal.operations.TimeoutConfiguration;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Scheduler;


public class MockOperationTimeoutConfiguration extends TimeoutConfiguration {

    public static final int TIMEOUT_IN_SEC = 30;

    public MockOperationTimeoutConfiguration(int seconds, Scheduler timeoutScheduler) {
        super(seconds, TimeUnit.SECONDS, timeoutScheduler);
    }

    public MockOperationTimeoutConfiguration(Scheduler timeoutScheduler) {
        this(TIMEOUT_IN_SEC, timeoutScheduler);
    }
}
