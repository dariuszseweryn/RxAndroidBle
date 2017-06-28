package com.polidea.rxandroidble.internal.util;

import com.polidea.rxandroidble.exceptions.BleIllegalOperationException;
import com.polidea.rxandroidble.internal.RxBleLog;

/**
 * Implementation of {@link IllegalOperationChecker}. This class logs an error and throws {@link BleIllegalOperationException} if there
 * was no match between possessed and requested properties.
 */
public class ThrowingIllegalOperationChecker extends IllegalOperationChecker {

    public ThrowingIllegalOperationChecker(@BluetoothGattCharacteristicProperty int propertyBroadcast,
                                           @BluetoothGattCharacteristicProperty int propertyRead,
                                           @BluetoothGattCharacteristicProperty int propertyWriteNoResponse,
                                           @BluetoothGattCharacteristicProperty int propertyWrite,
                                           @BluetoothGattCharacteristicProperty int propertyNotify,
                                           @BluetoothGattCharacteristicProperty int propertyIndicate,
                                           @BluetoothGattCharacteristicProperty int propertySignedWrite) {
        super(propertyBroadcast,
                propertyRead,
                propertyWriteNoResponse,
                propertyWrite, propertyNotify,
                propertyIndicate,
                propertySignedWrite);
    }

    /**
     * This method logs an error and throws a {@link BleIllegalOperationException}.
     * @param message the message displayed in the log and passed to exception
     */
    @Override
    protected void handleMessage(String message) {
        RxBleLog.e(message);
        throw new BleIllegalOperationException(message);
    }
}
