package com.polidea.rxandroidble3.mockrxandroidble.callbacks.results;

/**
 * An interface for the user to respond to a write request
 */
public interface RxBleGattWriteResultMock {
    /**
     * Respond with success
     */
    void success();

    /**
     * Respond with failure
     * @param status The ATT status (error code)
     */
    void failure(int status);

    /**
     * Trigger a disconnection
     * @param status The disconnection status (error code)
     */
    void disconnect(int status);
}
