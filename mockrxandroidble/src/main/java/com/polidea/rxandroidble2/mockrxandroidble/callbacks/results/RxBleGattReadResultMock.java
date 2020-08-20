package com.polidea.rxandroidble2.mockrxandroidble.callbacks.results;

/**
 * An interface for the user to respond to a read request
 */
public interface RxBleGattReadResultMock {
    /**
     * Respond with success
     * @param data The data to be returned in response to the read request
     */
    void success(byte[] data);

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
