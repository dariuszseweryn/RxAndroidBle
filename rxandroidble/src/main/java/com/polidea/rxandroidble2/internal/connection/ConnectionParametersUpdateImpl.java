package com.polidea.rxandroidble2.internal.connection;

import com.polidea.rxandroidble2.ConnectionParametersUpdate;


public class ConnectionParametersUpdateImpl implements ConnectionParametersUpdate {

    private final int interval, latency, timeout;

    ConnectionParametersUpdateImpl(int interval, int latency, int timeout) {
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
