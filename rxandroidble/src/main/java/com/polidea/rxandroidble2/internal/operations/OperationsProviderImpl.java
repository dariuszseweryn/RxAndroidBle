package com.polidea.rxandroidble2.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import androidx.annotation.RequiresApi;

import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.internal.connection.ConnectionModule;
import com.polidea.rxandroidble2.internal.connection.PayloadSizeLimitProvider;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble2.internal.logger.LoggerUtilBluetoothServices;

import java.util.concurrent.TimeUnit;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import bleshadow.javax.inject.Provider;

import io.reactivex.rxjava3.core.Scheduler;

public class OperationsProviderImpl implements OperationsProvider {

    private final RxBleGattCallback rxBleGattCallback;
    private final BluetoothGatt bluetoothGatt;
    private final LoggerUtilBluetoothServices bleServicesLogger;
    private final TimeoutConfiguration timeoutConfiguration;
    private final Scheduler bluetoothInteractionScheduler;
    private final Scheduler timeoutScheduler;
    private final Provider<ReadRssiOperation> rssiReadOperationProvider;

    @Inject
    OperationsProviderImpl(
            RxBleGattCallback rxBleGattCallback,
            BluetoothGatt bluetoothGatt,
            LoggerUtilBluetoothServices bleServicesLogger,
            @Named(ConnectionModule.OPERATION_TIMEOUT) TimeoutConfiguration timeoutConfiguration,
            @Named(ClientComponent.NamedSchedulers.BLUETOOTH_INTERACTION) Scheduler bluetoothInteractionScheduler,
            @Named(ClientComponent.NamedSchedulers.TIMEOUT) Scheduler timeoutScheduler,
            Provider<ReadRssiOperation> rssiReadOperationProvider) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.bleServicesLogger = bleServicesLogger;
        this.timeoutConfiguration = timeoutConfiguration;
        this.bluetoothInteractionScheduler = bluetoothInteractionScheduler;
        this.timeoutScheduler = timeoutScheduler;
        this.rssiReadOperationProvider = rssiReadOperationProvider;
    }

    @Override
    public CharacteristicLongWriteOperation provideLongWriteOperation(
            BluetoothGattCharacteristic bluetoothGattCharacteristic,
            RxBleConnection.WriteOperationAckStrategy writeOperationAckStrategy,
            RxBleConnection.WriteOperationRetryStrategy writeOperationRetryStrategy,
            PayloadSizeLimitProvider maxBatchSizeProvider,
            byte[] bytes) {

        return new CharacteristicLongWriteOperation(bluetoothGatt,
                rxBleGattCallback,
                bluetoothInteractionScheduler,
                timeoutConfiguration,
                bluetoothGattCharacteristic,
                maxBatchSizeProvider,
                writeOperationAckStrategy,
                writeOperationRetryStrategy,
                bytes);
    }

    @Override
    @RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
    public MtuRequestOperation provideMtuChangeOperation(int requestedMtu) {
        return new MtuRequestOperation(rxBleGattCallback, bluetoothGatt, timeoutConfiguration, requestedMtu);
    }

    @Override
    public CharacteristicReadOperation provideReadCharacteristic(BluetoothGattCharacteristic characteristic) {
        return new CharacteristicReadOperation(rxBleGattCallback, bluetoothGatt, timeoutConfiguration, characteristic);
    }

    @Override
    public DescriptorReadOperation provideReadDescriptor(BluetoothGattDescriptor descriptor) {
        return new DescriptorReadOperation(rxBleGattCallback, bluetoothGatt, timeoutConfiguration, descriptor);
    }

    @Override
    public ReadRssiOperation provideRssiReadOperation() {
        return rssiReadOperationProvider.get();
    }

    @Override
    public ServiceDiscoveryOperation provideServiceDiscoveryOperation(long timeout, TimeUnit timeUnit) {
        return new ServiceDiscoveryOperation(rxBleGattCallback, bluetoothGatt, bleServicesLogger,
                new TimeoutConfiguration(timeout, timeUnit, timeoutScheduler));
    }

    @Override
    public CharacteristicWriteOperation provideWriteCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
        return new CharacteristicWriteOperation(rxBleGattCallback, bluetoothGatt, timeoutConfiguration, characteristic, data);
    }

    @Override
    public DescriptorWriteOperation provideWriteDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor, byte[] data) {
        return new DescriptorWriteOperation(rxBleGattCallback, bluetoothGatt, timeoutConfiguration,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT, bluetoothGattDescriptor, data);
    }

    @Override
    @RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
    public ConnectionPriorityChangeOperation provideConnectionPriorityChangeOperation(int connectionPriority,
                                                                                      long delay,
                                                                                      TimeUnit timeUnit) {
        return new ConnectionPriorityChangeOperation(rxBleGattCallback, bluetoothGatt, timeoutConfiguration,
                connectionPriority, new TimeoutConfiguration(delay, timeUnit, timeoutScheduler));
    }
}
