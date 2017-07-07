package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble.ConnectionSetup;
import com.polidea.rxandroidble.internal.util.IllegalOperationChecker;
import com.polidea.rxandroidble.internal.util.LoggingMismatchDataHandler;
import com.polidea.rxandroidble.internal.util.ThrowingMismatchDataHandler;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

import static com.polidea.rxandroidble.internal.connection.ConnectionComponent.NamedBooleans.AUTO_CONNECT;

@Module
public class ConnectionModule {

    private boolean autoConnect;
    private boolean suppressOperationCheck;

    public ConnectionModule(ConnectionSetup connectionSetup) {
        this.autoConnect = connectionSetup.autoConnect;
        this.suppressOperationCheck = connectionSetup.suppressOperationCheck;
    }

    @ConnectionScope
    @Provides @Named(AUTO_CONNECT) boolean provideAutoConnect() {
        return autoConnect;
    }

    @Provides
    @ConnectionScope
    IllegalOperationChecker provideIllegalOperationChecker() {
        if (suppressOperationCheck) {
            return new IllegalOperationChecker(BluetoothGattCharacteristic.PROPERTY_BROADCAST,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PROPERTY_INDICATE,
                    BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE,
                    new LoggingMismatchDataHandler());
        } else {
            return new IllegalOperationChecker(BluetoothGattCharacteristic.PROPERTY_BROADCAST,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PROPERTY_INDICATE,
                    BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE,
                    new ThrowingMismatchDataHandler());
        }
    }
}