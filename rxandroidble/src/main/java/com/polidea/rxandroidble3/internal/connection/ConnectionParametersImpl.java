package com.polidea.rxandroidble3.internal.connection;

import com.polidea.rxandroidble3.ConnectionParameters;


public class ConnectionParametersImpl implements ConnectionParameters {

    private final int interval, latency, timeout;

    ConnectionParametersImpl(int interval, int latency, int timeout) {
        this.interval = interval;
        this.latency = latency;
        this.timeout = timeout;
    }

    @Override
    public int getConnectionInterval() {
        return interval;
    }

    @Override
    public int getSlaveLatency() {
        return latency;
    }

    @Override
    public int getSupervisionTimeout() {
        return timeout;
    }
}
