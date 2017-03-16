package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.DeviceModule;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import rx.Scheduler;

public class OperationsProviderImpl implements OperationsProvider {

    private final RxBleGattCallback rxBleGattCallback;
    private final BluetoothGatt bluetoothGatt;
    private final TimeoutConfiguration timeoutConfiguration;
    private final Scheduler mainThreadScheduler;
    private final Scheduler timeoutScheduler;
    private final Provider<RxBleRadioOperationReadRssi> rssiReadOperationProvider;

    @Inject
    OperationsProviderImpl(
            RxBleGattCallback rxBleGattCallback,
            BluetoothGatt bluetoothGatt,
            @Named(DeviceModule.OPERATION_TIMEOUT) TimeoutConfiguration timeoutConfiguration,
            @Named(ClientComponent.NamedSchedulers.MAIN_THREAD) Scheduler mainThreadScheduler,
            @Named(ClientComponent.NamedSchedulers.TIMEOUT) Scheduler timeoutScheduler,
            Provider<RxBleRadioOperationReadRssi> rssiReadOperationProvider) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.timeoutConfiguration = timeoutConfiguration;
        this.mainThreadScheduler = mainThreadScheduler;
        this.timeoutScheduler = timeoutScheduler;
        this.rssiReadOperationProvider = rssiReadOperationProvider;
    }

    @Override
    public RxBleRadioOperationCharacteristicLongWrite provideLongWriteOperation(
            BluetoothGattCharacteristic bluetoothGattCharacteristic,
            RxBleConnection.WriteOperationAckStrategy writeOperationAckStrategy,
            Callable<Integer> maxBatchSizeCallable,
            byte[] bytes) {

        return new RxBleRadioOperationCharacteristicLongWrite(bluetoothGatt,
                rxBleGattCallback,
                mainThreadScheduler,
                timeoutConfiguration,
                bluetoothGattCharacteristic,
                maxBatchSizeCallable,
                writeOperationAckStrategy,
                bytes);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RxBleRadioOperationMtuRequest provideMtuChangeOperation(int requestedMtu) {
        return new RxBleRadioOperationMtuRequest(rxBleGattCallback, bluetoothGatt, timeoutConfiguration, requestedMtu);
    }

    @Override
    public RxBleRadioOperationCharacteristicRead provideReadCharacteristic(BluetoothGattCharacteristic characteristic) {
        return new RxBleRadioOperationCharacteristicRead(rxBleGattCallback, bluetoothGatt, timeoutConfiguration, characteristic);
    }

    @Override
    public RxBleRadioOperationDescriptorRead provideReadDescriptor(BluetoothGattDescriptor descriptor) {
        return new RxBleRadioOperationDescriptorRead(rxBleGattCallback, bluetoothGatt, timeoutConfiguration, descriptor);
    }

    @Override
    public RxBleRadioOperationReadRssi provideRssiReadOperation() {
        return rssiReadOperationProvider.get();
    }

    @Override
    public RxBleRadioOperationServicesDiscover provideServiceDiscoveryOperation(long timeout, TimeUnit timeUnit) {
        return new RxBleRadioOperationServicesDiscover(rxBleGattCallback, bluetoothGatt,
                new TimeoutConfiguration(timeout, timeUnit, timeoutScheduler));
    }

    @Override
    public RxBleRadioOperationCharacteristicWrite provideWriteCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
        return new RxBleRadioOperationCharacteristicWrite(rxBleGattCallback, bluetoothGatt, timeoutConfiguration, characteristic, data);
    }

    @Override
    public RxBleRadioOperationDescriptorWrite provideWriteDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor, byte[] data) {
        return new RxBleRadioOperationDescriptorWrite(rxBleGattCallback, bluetoothGatt, timeoutConfiguration,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT, bluetoothGattDescriptor, data);
    }
}
