package com.polidea.rxandroidble.exceptions;

import com.polidea.rxandroidble.RxBleDevice;

/**
 * Exception emitted when unpairing a BLE device.
 *
 * @see RxBleDevice#unpair()
 */
public class BleDeviceUnpairException extends BleException {
    public BleDeviceUnpairException() {
        super();
    }

    public BleDeviceUnpairException(Throwable throwable) {
        super(throwable);
    }
}
