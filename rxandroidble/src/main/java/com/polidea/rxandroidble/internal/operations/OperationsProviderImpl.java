package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.DeviceModule;
import com.polidea.rxandroidble.internal.connection.PayloadSizeLimitProvider;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import rx.Scheduler;

public class OperationsProviderImpl implements OperationsProvider {

    private final RxBleGattCallback rxBleGattCallback;
    private final BluetoothGatt bluetoothGatt;
    private final TimeoutConfiguration timeoutConfiguration;
    private final Scheduler bluetoothInteractionScheduler;
    private final Scheduler timeoutScheduler;
    private final Provider<ReadRssiOperation> rssiReadOperationProvider;

    @Inject
    OperationsProviderImpl(
            RxBleGattCallback rxBleGattCallback,
            BluetoothGatt bluetoothGatt,
            @Named(DeviceModule.OPERATION_TIMEOUT) TimeoutConfiguration timeoutConfiguration,
            @Named(ClientComponent.NamedSchedulers.BLUETOOTH_INTERACTION) Scheduler bluetoothInteractionScheduler,
            @Named(ClientComponent.NamedSchedulers.TIMEOUT) Scheduler timeoutScheduler,
            Provider<ReadRssiOperation> rssiReadOperationProvider) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.timeoutConfiguration = timeoutConfiguration;
        this.bluetoothInteractionScheduler = bluetoothInteractionScheduler;
        this.timeoutScheduler = timeoutScheduler;
        this.rssiReadOperationProvider = rssiReadOperationProvider;
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
        return new ServiceDiscoveryOperation(rxBleGattCallback, bluetoothGatt,
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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ConnectionPriorityChangeOperation provideConnectionPriorityChangeOperation(int connectionPriority,
                                                                                      long delay,
                                                                                      TimeUnit timeUnit) {
        return new ConnectionPriorityChangeOperation(rxBleGattCallback, bluetoothGatt, timeoutConfiguration,
                connectionPriority, delay, timeUnit, timeoutScheduler);
    }
}
