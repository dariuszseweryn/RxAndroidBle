package com.polidea.rxandroidble2.mockrxandroidble.callbacks;

import android.bluetooth.BluetoothGattDescriptor;

import com.polidea.rxandroidble2.mockrxandroidble.callbacks.results.RxBleGattReadResultMock;
import com.polidea.rxandroidble2.mockrxandroidble.RxBleDeviceMock;

/**
 * An interface for a user callback for handling descriptor read requests
 */
public interface RxBleDescriptorReadCallback {

    /**
     * Handles a read on a GATT descriptor
     * @param device the device being read from
     * @param descriptor the descriptor being read from
     * @param result the result handler
     * @throws Exception on error
     */
    void handle(RxBleDeviceMock device, BluetoothGattDescriptor descriptor, RxBleGattReadResultMock result) throws Exception;
}
