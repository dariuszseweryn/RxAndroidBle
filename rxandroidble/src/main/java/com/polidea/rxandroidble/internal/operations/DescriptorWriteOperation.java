package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.eventlog.OperationAttribute;
import com.polidea.rxandroidble.eventlog.OperationDescription;
import com.polidea.rxandroidble.eventlog.OperationEventLogger;
import com.polidea.rxandroidble.eventlog.OperationExtras;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.SingleResponseOperation;
import com.polidea.rxandroidble.internal.connection.ConnectionModule;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.ByteAssociation;
import com.polidea.rxandroidble.utils.BytePrinter;

import bleshadow.javax.inject.Named;
import rx.Observable;
import rx.functions.Func1;

public class DescriptorWriteOperation extends SingleResponseOperation<byte[]> {

    private BluetoothGattDescriptor bluetoothGattDescriptor;
    private byte[] data;
    private final int bluetoothGattCharacteristicDefaultWriteType;

    DescriptorWriteOperation(RxBleGattCallback rxBleGattCallback,
                             BluetoothGatt bluetoothGatt,
                             @Named(ConnectionModule.OPERATION_TIMEOUT) TimeoutConfiguration timeoutConfiguration,
                             int bluetoothGattCharacteristicDefaultWriteType,
                             BluetoothGattDescriptor bluetoothGattDescriptor,
                             byte[] data,
                             OperationEventLogger eventLogger) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.DESCRIPTOR_WRITE, timeoutConfiguration, eventLogger);
        this.bluetoothGattCharacteristicDefaultWriteType = bluetoothGattCharacteristicDefaultWriteType;
        this.bluetoothGattDescriptor = bluetoothGattDescriptor;
        this.data = data;
    }

    @Override
    protected Observable<byte[]> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback
                .getOnDescriptorWrite()
                .filter(new Func1<ByteAssociation<BluetoothGattDescriptor>, Boolean>() {
                    @Override
                    public Boolean call(ByteAssociation<BluetoothGattDescriptor> uuidPair) {
                        return uuidPair.first.equals(bluetoothGattDescriptor);
                    }
                })
                .map(new Func1<ByteAssociation<BluetoothGattDescriptor>, byte[]>() {
                    @Override
                    public byte[] call(ByteAssociation<BluetoothGattDescriptor> uuidPair) {
                        return uuidPair.second;
                    }
                });
    }

    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) {
        bluetoothGattDescriptor.setValue(data);

        /*
        * According to the source code below Android 7.0.0 the BluetoothGatt.writeDescriptor() function used
        * writeType of the parent BluetoothCharacteristic which caused operation failure (for instance when
        * setting Client Characteristic Config). With WRITE_TYPE_DEFAULT problem did not occurred.
        * Compare:
        * https://android.googlesource.com/platform/frameworks/base/+/android-6.0.1_r74/core/java/android/bluetooth/BluetoothGatt.java#1039
        * https://android.googlesource.com/platform/frameworks/base/+/android-7.0.0_r1/core/java/android/bluetooth/BluetoothGatt.java#947
        */
        final BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattDescriptor.getCharacteristic();
        final int originalWriteType = bluetoothGattCharacteristic.getWriteType();
        bluetoothGattCharacteristic.setWriteType(bluetoothGattCharacteristicDefaultWriteType);

        final boolean success = bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
        bluetoothGattCharacteristic.setWriteType(originalWriteType);
        return success;
    }

    @NonNull
    @Override
    protected OperationDescription createOperationDescription() {
        return new OperationDescription(
                new OperationAttribute(OperationExtras.UUID, bluetoothGattDescriptor.getUuid().toString()),
                new OperationAttribute(OperationExtras.WRITE_TYPE, String.valueOf(bluetoothGattCharacteristicDefaultWriteType)),
                new OperationAttribute(OperationExtras.DATA, BytePrinter.toPrettyFormattedHexString(data))
        );
    }

    @Nullable
    @Override
    protected String createOperationResultDescription(byte[] result) {
        return BytePrinter.toPrettyFormattedHexString(result);
    }
}
