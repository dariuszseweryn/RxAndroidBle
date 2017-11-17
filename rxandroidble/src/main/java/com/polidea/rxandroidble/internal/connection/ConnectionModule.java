package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.ConnectionSetup;
import com.polidea.rxandroidble.TimeoutSetup;
import com.polidea.rxandroidble.internal.operations.TimeoutConfiguration;
import com.polidea.rxandroidble.internal.util.CharacteristicPropertiesParser;

import javax.inject.Named;
import javax.inject.Provider;

import dagger.Module;
import dagger.Provides;
import rx.Scheduler;

import static com.polidea.rxandroidble.internal.connection.ConnectionComponent.NamedBooleans.AUTO_CONNECT;

@Module
public class ConnectionModule {

    public static final String OPERATION_TIMEOUT = "operation-timeout";
    final boolean autoConnect;
    final boolean suppressOperationCheck;
    private final TimeoutSetup operationTimeout;

    ConnectionModule(ConnectionSetup connectionSetup) {
        this.autoConnect = connectionSetup.autoConnect;
        this.suppressOperationCheck = connectionSetup.suppressOperationCheck;
        this.operationTimeout = connectionSetup.operationTimeout;
    }

    @ConnectionScope
    @Provides @Named(AUTO_CONNECT) boolean provideAutoConnect() {
        return autoConnect;
    }


    @Provides
    @Named(OPERATION_TIMEOUT)
    TimeoutConfiguration providesOperationTimeoutConf(@Named(ClientComponent.NamedSchedulers.TIMEOUT) Scheduler timeoutScheduler) {
        return new TimeoutConfiguration(operationTimeout.timeout, operationTimeout.timeUnit, timeoutScheduler);
    }

    @Provides
    IllegalOperationHandler provideIllegalOperationHandler(
            Provider<LoggingIllegalOperationHandler> loggingIllegalOperationHandlerProvider,
            Provider<ThrowingIllegalOperationHandler> throwingIllegalOperationHandlerProvider
            ) {
        if (suppressOperationCheck) {
            return loggingIllegalOperationHandlerProvider.get();
        } else {
            return throwingIllegalOperationHandlerProvider.get();
        }
    }

    @Provides
    CharacteristicPropertiesParser provideCharacteristicPropertiesParser() {
        return new CharacteristicPropertiesParser(BluetoothGattCharacteristic.PROPERTY_BROADCAST,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE);
    }
}