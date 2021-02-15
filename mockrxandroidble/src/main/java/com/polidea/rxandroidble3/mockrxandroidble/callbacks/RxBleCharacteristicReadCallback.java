package com.polidea.rxandroidble2.mockrxandroidble.callbacks;

import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble2.mockrxandroidble.callbacks.results.RxBleGattReadResultMock;
import com.polidea.rxandroidble2.mockrxandroidble.RxBleDeviceMock;

/**
 * An interface for a user callback for handling characteristic read requests
 */
public interface RxBleCharacteristicReadCallback {

    /**
     * Handles a read on a GATT characteristic
     * @param device the device being read from
     * @param characteristic the characteristic being read from
     * @param result the result handler
     * @throws Exception on error
     */
    void handle(RxBleDeviceMock device, BluetoothGattCharacteristic characteristic, RxBleGattReadResultMock result) throws Exception;
}
