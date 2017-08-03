package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.DeviceModule;
import com.polidea.rxandroidble.internal.RxBleSingleGattRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.ByteAssociation;

import java.util.UUID;

import javax.inject.Named;

import rx.Observable;
import rx.functions.Func1;

public class RxBleRadioOperationCharacteristicWrite extends RxBleSingleGattRadioOperation<byte[]> {

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private final byte[] data;

    RxBleRadioOperationCharacteristicWrite(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                           @Named(DeviceModule.OPERATION_TIMEOUT) TimeoutConfiguration timeoutConfiguration,
                                           BluetoothGattCharacteristic bluetoothGattCharacteristic,
                                           byte[] data) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.CHARACTERISTIC_WRITE, timeoutConfiguration);
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.data = data;
    }

    @Override
    protected Observable<byte[]> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback
                .getOnCharacteristicWrite()
                .filter(new Func1<ByteAssociation<UUID>, Boolean>() {
                    @Override
                    public Boolean call(ByteAssociation<UUID> uuidPair) {
                        return uuidPair.first.equals(bluetoothGattCharacteristic.getUuid());
                    }
                })
                .map(new Func1<ByteAssociation<UUID>, byte[]>() {
                    @Override
                    public byte[] call(ByteAssociation<UUID> uuidPair) {
                        return uuidPair.second;
                    }
                });
    }

    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) {
        bluetoothGattCharacteristic.setValue(data);
        return bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
    }
}
