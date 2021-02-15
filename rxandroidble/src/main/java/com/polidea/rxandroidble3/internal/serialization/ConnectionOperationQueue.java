package com.polidea.rxandroidble2.internal.serialization;

import com.polidea.rxandroidble2.exceptions.BleException;

/**
 * {@inheritDoc}
 */
public interface ConnectionOperationQueue extends ClientOperationQueue {

    /**
     * A method for terminating all operations that are still queued on the connection.
     * @param disconnectedException the exception to be passed to all queued operations subscribers
     */
    void terminate(BleException disconnectedException);
}
