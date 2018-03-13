package com.polidea.rxandroidble2.internal.connection;

import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.ConnectionSetup;
import com.polidea.rxandroidble2.Timeout;
import com.polidea.rxandroidble2.internal.operations.TimeoutConfiguration;
import com.polidea.rxandroidble2.internal.util.CharacteristicPropertiesParser;

import bleshadow.dagger.Module;
import bleshadow.dagger.Provides;
import bleshadow.javax.inject.Named;
import bleshadow.javax.inject.Provider;
import io.reactivex.Scheduler;

import static com.polidea.rxandroidble2.internal.connection.ConnectionComponent.NamedBooleans.AUTO_CONNECT;

@Module
public class ConnectionModule {

    public static final String OPERATION_TIMEOUT = "operation-timeout";
    final boolean autoConnect;
    final boolean suppressOperationCheck;
    private final Timeout operationTimeout;

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