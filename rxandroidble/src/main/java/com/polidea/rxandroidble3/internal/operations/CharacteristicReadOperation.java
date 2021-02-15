package com.polidea.rxandroidble3.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble3.exceptions.BleGattOperationType;
import com.polidea.rxandroidble3.internal.SingleResponseOperation;
import com.polidea.rxandroidble3.internal.connection.ConnectionModule;
import com.polidea.rxandroidble3.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble3.internal.logger.LoggerUtil;

import bleshadow.javax.inject.Named;

import io.reactivex.rxjava3.core.Single;

import static com.polidea.rxandroidble3.internal.util.ByteAssociationUtil.characteristicUUIDPredicate;
import static com.polidea.rxandroidble3.internal.util.ByteAssociationUtil.getBytesFromAssociation;

public class CharacteristicReadOperation extends SingleResponseOperation<byte[]> {

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;

    CharacteristicReadOperation(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                @Named(ConnectionModule.OPERATION_TIMEOUT) TimeoutConfiguration timeoutConfiguration,
                                BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.CHARACTERISTIC_READ, timeoutConfiguration);
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
    }

    @Override
    protected Single<byte[]> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback
                .getOnCharacteristicRead()
                .filter(characteristicUUIDPredicate(bluetoothGattCharacteristic.getUuid()))
                .firstOrError()
                .map(getBytesFromAssociation());
    }

    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) {
        return bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
    }

    @Override
    public String toString() {
        return "CharacteristicReadOperation{"
                + super.toString()
                + ", characteristic=" + LoggerUtil.wrap(bluetoothGattCharacteristic, false)
                + '}';
    }
}
