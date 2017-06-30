package com.polidea.rxandroidble;


@SuppressWarnings("WeakerAccess")
public class ConnectionParams {

    public final int connectionInterval;

    public final int slaveLatency;

    public final int supervisionTimeout;

    public ConnectionParams(int connectionInterval, int slaveLatency, int supervisionTimeout) {
        this.connectionInterval = connectionInterval;
        this.slaveLatency = slaveLatency;
        this.supervisionTimeout = supervisionTimeout;
    }
}
