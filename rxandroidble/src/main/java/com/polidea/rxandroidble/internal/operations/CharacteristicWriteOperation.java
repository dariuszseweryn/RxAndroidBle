package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.SingleResponseOperation;
import com.polidea.rxandroidble.internal.connection.ConnectionModule;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import bleshadow.javax.inject.Named;
import io.reactivex.Single;

import static com.polidea.rxandroidble.internal.util.ByteAssociationUtil.characteristicUUIDPredicate;
import static com.polidea.rxandroidble.internal.util.ByteAssociationUtil.getBytesFromAssociation;

public class CharacteristicWriteOperation extends SingleResponseOperation<byte[]> {

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private final byte[] data;

    CharacteristicWriteOperation(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                 @Named(ConnectionModule.OPERATION_TIMEOUT) TimeoutConfiguration timeoutConfiguration,
                                 BluetoothGattCharacteristic bluetoothGattCharacteristic,
                                 byte[] data) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.CHARACTERISTIC_WRITE, timeoutConfiguration);
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.data = data;
    }

    @Override
    protected Single<byte[]> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback
                .getOnCharacteristicWrite()
                .filter(characteristicUUIDPredicate(bluetoothGattCharacteristic.getUuid()))
                .firstOrError()
                .map(getBytesFromAssociation());
    }

    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) {
        bluetoothGattCharacteristic.setValue(data);
        return bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
    }
}
