package com.polidea.rxandroidble2.mockrxandroidble.Callbacks;

import android.bluetooth.BluetoothGattDescriptor;

import com.polidea.rxandroidble2.mockrxandroidble.Callbacks.Results.RxBleGattReadResultMock;
import com.polidea.rxandroidble2.mockrxandroidble.RxBleDeviceMock;

/**
 * An interface for a user callback for handling descriptor read requests
 */
public interface RxBleDescriptorReadCallback extends RxBleReadCallback<BluetoothGattDescriptor> {

    /**
     * Handles a read on a GATT descriptor
     * @param device the device being read from
     * @param descriptor the descriptor being read from
     * @param result the result handler
     * @throws Exception on error
     */
    @Override
    void handle(RxBleDeviceMock device, BluetoothGattDescriptor descriptor, RxBleGattReadResultMock result) throws Exception;
}
