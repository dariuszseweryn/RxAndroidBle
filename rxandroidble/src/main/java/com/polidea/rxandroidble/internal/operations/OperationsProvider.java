package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.polidea.rxandroidble.RxBleConnection;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public interface OperationsProvider {

    RxBleRadioOperationCharacteristicLongWrite provideLongWriteOperation(
            BluetoothGattCharacteristic bluetoothGattCharacteristic,
            RxBleConnection.WriteOperationAckStrategy writeOperationAckStrategy,
            Callable<Integer> maxBatchSizeCallable,
            byte[] bytes);

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    RxBleRadioOperationMtuRequest provideMtuChangeOperation(int requestedMtu);

    RxBleRadioOperationCharacteristicRead provideReadCharacteristic(BluetoothGattCharacteristic characteristic);

    RxBleRadioOperationDescriptorRead provideReadDescriptor(BluetoothGattDescriptor descriptor);

    RxBleRadioOperationReadRssi provideRssiReadOperation();

    RxBleRadioOperationServicesDiscover provideServiceDiscoveryOperation(long timeout, TimeUnit timeUnit);

    RxBleRadioOperationCharacteristicWrite provideWriteCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data);

    RxBleRadioOperationDescriptorWrite provideWriteDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor, byte[] data);
}
