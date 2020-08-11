package com.polidea.rxandroidble2.mockrxandroidble.Callbacks;

import com.polidea.rxandroidble2.mockrxandroidble.Callbacks.Results.RxBleGattReadResultMock;
import com.polidea.rxandroidble2.mockrxandroidble.RxBleDeviceMock;

import io.reactivex.annotations.NonNull;

/**
 * A generic interface for a user callback for handling read requests
 * @param <T> The type of attribute ({@link android.bluetooth.BluetoothGattCharacteristic} or
 * {@link android.bluetooth.BluetoothGattDescriptor})
 */
public interface RxBleReadCallback<T> {
    /**
     * Handles a read on a GATT attribute
     * @param device the device being read from
     * @param attribute the attribute being read from
     * @param result the result handler
     * @throws Exception on error
     */
    void handle(@NonNull RxBleDeviceMock device, @NonNull T attribute, @NonNull RxBleGattReadResultMock result) throws Exception;
}
