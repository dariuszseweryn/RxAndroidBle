package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.operations.OperationsProvider;
import com.polidea.rxandroidble.internal.operations.OperationsProviderImpl;

import java.util.concurrent.Callable;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

@Module
public class ConnectionModule {

    public static final String CURRENT_MTU = "current-mtu";

    @Provides
    @Named(CURRENT_MTU)
    Callable<Integer> provideCurrentMtuProvider(final RxBleConnectionImpl rxBleConnection) {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return rxBleConnection.currentMtu;
            }
        };
    }

    @Provides
    RxBleConnection.LongWriteOperationBuilder provideLongWriteOperationBuilder(LongWriteOperationBuilderImpl operationBuilder) {
        return operationBuilder;
    }

    @Provides
    OperationsProvider provideOperationsProvider(OperationsProviderImpl operationsProvider) {
        return operationsProvider;
    }

    @Provides
    @ConnectionScope
    RxBleConnection provideRxBleConnection(RxBleConnectionImpl rxBleConnection) {
        return rxBleConnection;
    }

    @Provides
    @ConnectionScope
    BluetoothGatt provideBluetoothGatt(BluetoothGattProvider bluetoothGattProvider) {
        return bluetoothGattProvider.getBluetoothGatt();
    }
}
