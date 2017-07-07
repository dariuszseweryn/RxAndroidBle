package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble.ConnectionSetup;

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
        IllegalOperationHandler dataHandler;
        if (suppressOperationCheck) {
            dataHandler = new LoggingIllegalOperationHandler();
        } else {
            dataHandler = new ThrowingIllegalOperationHandler();
        }
        return new IllegalOperationChecker(BluetoothGattCharacteristic.PROPERTY_BROADCAST,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE,
                dataHandler);
    }
}