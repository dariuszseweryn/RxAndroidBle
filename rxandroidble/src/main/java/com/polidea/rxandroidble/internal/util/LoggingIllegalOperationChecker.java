package com.polidea.rxandroidble.internal.util;

import com.polidea.rxandroidble.internal.RxBleLog;

/**
 * Implementation of {@link IllegalOperationChecker}. This class logs a warning if there was no match between possessed
 * and requested properties.
 */
public class LoggingIllegalOperationChecker extends IllegalOperationChecker {

    public LoggingIllegalOperationChecker(@BluetoothGattCharacteristicProperty int propertyBroadcast,
                                          @BluetoothGattCharacteristicProperty int propertyRead,
                                          @BluetoothGattCharacteristicProperty int propertyWriteNoResponse,
                                          @BluetoothGattCharacteristicProperty int propertyWrite,
                                          @BluetoothGattCharacteristicProperty int propertyNotify,
                                          @BluetoothGattCharacteristicProperty int propertyIndicate,
                                          @BluetoothGattCharacteristicProperty int propertySignedWrite) {
        super(propertyBroadcast,
                propertyRead,
                propertyWriteNoResponse,
                propertyWrite,
                propertyNotify,
                propertyIndicate,
                propertySignedWrite);
    }

    /**
     * This method logs a warning.
     * @param message the message displayed in the warning log
     */
    @Override
    protected void handleMessage(String message) {
        RxBleLog.w(message);
    }
}
