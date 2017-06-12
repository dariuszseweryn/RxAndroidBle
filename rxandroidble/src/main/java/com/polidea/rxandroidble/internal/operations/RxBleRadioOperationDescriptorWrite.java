package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.DeviceModule;
import com.polidea.rxandroidble.internal.RxBleSingleGattRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.ByteAssociation;

import javax.inject.Named;

import rx.Observable;
import rx.functions.Func1;

public class RxBleRadioOperationDescriptorWrite extends RxBleSingleGattRadioOperation<byte[]> {

    private BluetoothGattDescriptor bluetoothGattDescriptor;
    private byte[] data;
    private final int bluetoothGattCharacteristicDefaultWriteType;

    RxBleRadioOperationDescriptorWrite(RxBleGattCallback rxBleGattCallback,
                                       BluetoothGatt bluetoothGatt,
                                       @Named(DeviceModule.OPERATION_TIMEOUT) TimeoutConfiguration timeoutConfiguration,
                                       int bluetoothGattCharacteristicDefaultWriteType,
                                       BluetoothGattDescriptor bluetoothGattDescriptor,
                                       byte[] data) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.DESCRIPTOR_WRITE, timeoutConfiguration);
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
}
