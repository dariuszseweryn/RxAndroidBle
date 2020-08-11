package com.polidea.rxandroidble2.mockrxandroidble.Callbacks;


import com.polidea.rxandroidble2.mockrxandroidble.Callbacks.Results.RxBleGattWriteResultMock;
import com.polidea.rxandroidble2.mockrxandroidble.RxBleDeviceMock;

import io.reactivex.annotations.NonNull;

/**
 * A generic interface for a user callback for handling write requests
 * @param <T> The type of attribute ({@link android.bluetooth.BluetoothGattCharacteristic} or {@link android.bluetooth.BluetoothGattDescriptor})
 */
public interface RxBleWriteCallback<T> {
    /**
     * Handles a write on a GATT attribute
     * @param device the device being written to
     * @param attribute the attribute being written to
     * @param data the data being written
     * @param result the result handler
     * @throws Exception on error
     */
    void handle(@NonNull RxBleDeviceMock device,
                @NonNull T attribute,
                @NonNull byte[] data,
                @NonNull RxBleGattWriteResultMock result) throws Exception;
}
