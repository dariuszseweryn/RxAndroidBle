package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.operations.OperationsProvider;
import com.polidea.rxandroidble.internal.operations.OperationsProviderImpl;
import com.polidea.rxandroidble.internal.serialization.ConnectionOperationQueue;
import com.polidea.rxandroidble.internal.serialization.ConnectionOperationQueueImpl;

import dagger.Binds;
import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

import static com.polidea.rxandroidble.internal.connection.ConnectionComponent.NamedInts.GATT_WRITE_MTU_OVERHEAD;

@Module
abstract public class ConnectionModuleBinder {

    @Provides
    @Named(GATT_WRITE_MTU_OVERHEAD)
    static int gattWriteMtuOverhead() {
        return RxBleConnection.GATT_WRITE_MTU_OVERHEAD;
    }

    @Provides
    @ConnectionScope
    static BluetoothGatt provideBluetoothGatt(BluetoothGattProvider bluetoothGattProvider) {
        return bluetoothGattProvider.getBluetoothGatt();
    }

    @Binds
    abstract RxBleConnection.LongWriteOperationBuilder bindLongWriteOperationBuilder(LongWriteOperationBuilderImpl operationBuilder);

    @Binds
    abstract OperationsProvider bindOperationsProvider(OperationsProviderImpl operationsProvider);

    @Binds
    @ConnectionScope
    abstract RxBleConnection bindRxBleConnection(RxBleConnectionImpl rxBleConnection);

    @Binds
    @ConnectionScope
    abstract ConnectionOperationQueue bindConnectionOperationQueue(ConnectionOperationQueueImpl connectionOperationQueue);
}
