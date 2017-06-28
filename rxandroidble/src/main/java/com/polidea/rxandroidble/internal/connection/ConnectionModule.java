package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble.internal.util.IllegalOperationChecker;
import com.polidea.rxandroidble.internal.util.LoggingIllegalOperationChecker;
import com.polidea.rxandroidble.internal.util.ThrowingIllegalOperationChecker;

import dagger.Module;
import dagger.Provides;

@Module
public class ConnectionModule {

    private boolean suppressPropertiesCheck;

    public ConnectionModule(boolean suppressPropertiesCheck) {
        this.suppressPropertiesCheck = suppressPropertiesCheck;
    }

    @Provides
    @ConnectionScope
    IllegalOperationChecker provideIllegalOperationChecker() {
        if (suppressPropertiesCheck) {
            return new LoggingIllegalOperationChecker(BluetoothGattCharacteristic.PROPERTY_BROADCAST,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PROPERTY_INDICATE,
                    BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE);
        } else {
            return new ThrowingIllegalOperationChecker(BluetoothGattCharacteristic.PROPERTY_BROADCAST,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PROPERTY_INDICATE,
                    BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE);
        }
    }
}