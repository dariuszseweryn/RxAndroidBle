package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.connection.PayloadSizeLimitProvider;

import java.util.concurrent.TimeUnit;

public interface OperationsProvider {

    CharacteristicLongWriteOperation provideLongWriteOperation(
            BluetoothGattCharacteristic bluetoothGattCharacteristic,
            RxBleConnection.WriteOperationAckStrategy writeOperationAckStrategy,
            PayloadSizeLimitProvider maxBatchSizeProvider,
            byte[] bytes);

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    MtuRequestOperation provideMtuChangeOperation(int requestedMtu);

    CharacteristicReadOperation provideReadCharacteristic(BluetoothGattCharacteristic characteristic);

    DescriptorReadOperation provideReadDescriptor(BluetoothGattDescriptor descriptor);

    ReadRssiOperation provideRssiReadOperation();

    ServiceDiscoveryOperation provideServiceDiscoveryOperation(long timeout, TimeUnit timeUnit);

    CharacteristicWriteOperation provideWriteCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data);

    DescriptorWriteOperation provideWriteDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor, byte[] data);

    ConnectionPriorityChangeOperation provideConnectionPriorityChangeOperation(
            int connectionPriority,
            long delay,
            TimeUnit timeUnit
    );
}
