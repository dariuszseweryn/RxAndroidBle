package com.polidea.rxandroidble3;

import androidx.annotation.IntRange;

/**
 * An interface representing connection parameters update
 */
public interface ConnectionParameters {

    /**
     * Returns the connection interval used on this connection, 1.25ms unit. Valid range is from 6 (7.5ms) to 3200 (4000ms)
     *
     * @return the connection interval
     */
    @IntRange(from = 6, to = 3200)
    int getConnectionInterval();

    /**
     * Returns the slave latency for the connection in number of connection events. Valid range is from 0 to 499
     *
     * @return the slave latency
     */
    @IntRange(from = 0, to = 499)
    int getSlaveLatency();

    /**
     * Returns the supervision timeout for this connection, in 10ms unit. Valid range is from 10 (0.1s) to 3200 (32s)
     *
     * @return the supervision timeout
     */
    @IntRange(from = 10, to = 3200)
    int getSupervisionTimeout();
}
