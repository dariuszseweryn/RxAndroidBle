package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.eventlog.OperationEventLogger;
import com.polidea.rxandroidble.internal.connection.ConnectionModule;
import com.polidea.rxandroidble.internal.connection.PayloadSizeLimitProvider;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.RxBleServicesLogger;

import java.util.concurrent.TimeUnit;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import bleshadow.javax.inject.Provider;

import rx.Scheduler;

public class OperationsProviderImpl implements OperationsProvider {

    private final RxBleGattCallback rxBleGattCallback;
    private final BluetoothGatt bluetoothGatt;
    private final RxBleServicesLogger bleServicesLogger;
    private final TimeoutConfiguration timeoutConfiguration;
    private final Scheduler bluetoothInteractionScheduler;
    private final Scheduler timeoutScheduler;
    private final Provider<ReadRssiOperation> rssiReadOperationProvider;
    private final OperationEventLogger eventLogger;

    @Inject
    OperationsProviderImpl(
            RxBleGattCallback rxBleGattCallback,
            BluetoothGatt bluetoothGatt,
            RxBleServicesLogger bleServicesLogger,
            @Named(ConnectionModule.OPERATION_TIMEOUT) TimeoutConfiguration timeoutConfiguration,
            @Named(ClientComponent.NamedSchedulers.BLUETOOTH_INTERACTION) Scheduler bluetoothInteractionScheduler,
            @Named(ClientComponent.NamedSchedulers.TIMEOUT) Scheduler timeoutScheduler,
            Provider<ReadRssiOperation> rssiReadOperationProvider,
            OperationEventLogger eventLogger) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.bleServicesLogger = bleServicesLogger;
        this.timeoutConfiguration = timeoutConfiguration;
        this.bluetoothInteractionScheduler = bluetoothInteractionScheduler;
        this.timeoutScheduler = timeoutScheduler;
        this.rssiReadOperationProvider = rssiReadOperationProvider;
        this.eventLogger = eventLogger;
    }

    @Override
    public CharacteristicLongWriteOperation provideLongWriteOperation(
            BluetoothGattCharacteristic bluetoothGattCharacteristic,
            RxBleConnection.WriteOperationAckStrategy writeOperationAckStrategy,
            PayloadSizeLimitProvider maxBatchSizeProvider,
            byte[] bytes) {

        return new CharacteristicLongWriteOperation(bluetoothGatt,
                rxBleGattCallback,
                bluetoothInteractionScheduler,
                timeoutConfiguration,
                bluetoothGattCharacteristic,
                maxBatchSizeProvider,
                writeOperationAckStrategy,
                bytes);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public MtuRequestOperation provideMtuChangeOperation(int requestedMtu) {
        return new MtuRequestOperation(rxBleGattCallback, bluetoothGatt, timeoutConfiguration, requestedMtu, eventLogger);
    }

    @Override
    public CharacteristicReadOperation provideReadCharacteristic(BluetoothGattCharacteristic characteristic) {
        return new CharacteristicReadOperation(rxBleGattCallback, bluetoothGatt, timeoutConfiguration, characteristic, eventLogger);
    }

    @Override
    public DescriptorReadOperation provideReadDescriptor(BluetoothGattDescriptor descriptor) {
        return new DescriptorReadOperation(rxBleGattCallback, bluetoothGatt, timeoutConfiguration, descriptor, eventLogger);
    }

    @Override
    public ReadRssiOperation provideRssiReadOperation() {
        return rssiReadOperationProvider.get();
    }

    @Override
    public ServiceDiscoveryOperation provideServiceDiscoveryOperation(long timeout, TimeUnit timeUnit) {
        return new ServiceDiscoveryOperation(rxBleGattCallback, bluetoothGatt, bleServicesLogger,
                new TimeoutConfiguration(timeout, timeUnit, timeoutScheduler), eventLogger);
    }

    @Override
    public CharacteristicWriteOperation provideWriteCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
        return new CharacteristicWriteOperation(rxBleGattCallback, bluetoothGatt, timeoutConfiguration, characteristic, data, eventLogger);
    }

    @Override
    public DescriptorWriteOperation provideWriteDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor, byte[] data) {
        return new DescriptorWriteOperation(rxBleGattCallback, bluetoothGatt, timeoutConfiguration,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT, bluetoothGattDescriptor, data, eventLogger);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ConnectionPriorityChangeOperation provideConnectionPriorityChangeOperation(int connectionPriority,
                                                                                      long delay,
                                                                                      TimeUnit timeUnit) {
        return new ConnectionPriorityChangeOperation(rxBleGattCallback, bluetoothGatt, timeoutConfiguration,
                connectionPriority, delay, timeUnit, timeoutScheduler, eventLogger);
    }
}
