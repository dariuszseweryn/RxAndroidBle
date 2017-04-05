package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.operations.OperationsProvider;
import com.polidea.rxandroidble.internal.operations.OperationsProviderImpl;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;

@Module
public class ConnectionModule {

    private static final String CURRENT_MTU_PROVIDER = "current-mtu-provider";
    static final String CURRENT_MAX_WRITE_PAYLOAD_SIZE_PROVIDER = "current-max-write-batch-size-provider";

    @Provides
    @Named(CURRENT_MTU_PROVIDER)
    IntProvider provideMtuProvider(RxBleConnectionImpl rxBleConnectionImpl) {
        return rxBleConnectionImpl;
    }

    @Provides
    @ConnectionScope
    @Named(CURRENT_MAX_WRITE_PAYLOAD_SIZE_PROVIDER)
    IntProvider provideWriteBatchSizeProvider(@Named(CURRENT_MTU_PROVIDER) IntProvider mtuProvider) {
        return new MaxWritePayloadSizeProvider(mtuProvider, RxBleConnection.GATT_WRITE_MTU_OVERHEAD);
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
