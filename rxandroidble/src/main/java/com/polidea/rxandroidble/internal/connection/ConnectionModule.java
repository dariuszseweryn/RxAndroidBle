package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.operations.OperationsProvider;
import com.polidea.rxandroidble.internal.operations.OperationsProviderImpl;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

@Module
public class ConnectionModule {

    static final String GATT_WRITE_MTU_OVERHEAD = "GATT_WRITE_MTU_OVERHEAD";

    @Provides
    @Named(GATT_WRITE_MTU_OVERHEAD)
    int gattWriteMtuOverhead() {
        return RxBleConnection.GATT_WRITE_MTU_OVERHEAD;
    }

    @Provides
    @ConnectionScope
    BluetoothGatt provideBluetoothGatt(BluetoothGattProvider bluetoothGattProvider) {
        return bluetoothGattProvider.getBluetoothGatt();
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
}
