package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.internal.eventlog.OperationAttribute;
import com.polidea.rxandroidble.internal.eventlog.OperationDescription;
import com.polidea.rxandroidble.internal.eventlog.OperationEventLogger;
import com.polidea.rxandroidble.internal.eventlog.OperationExtras;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.SingleResponseOperation;
import com.polidea.rxandroidble.internal.connection.ConnectionModule;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.ByteAssociation;
import com.polidea.rxandroidble.utils.BytePrinter;

import java.util.UUID;

import bleshadow.javax.inject.Named;
import rx.Observable;
import rx.functions.Func1;

public class CharacteristicWriteOperation extends SingleResponseOperation<byte[]> {

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private final byte[] data;

    CharacteristicWriteOperation(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                 @Named(ConnectionModule.OPERATION_TIMEOUT) TimeoutConfiguration timeoutConfiguration,
                                 BluetoothGattCharacteristic bluetoothGattCharacteristic,
                                 byte[] data, OperationEventLogger eventLogger) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.CHARACTERISTIC_WRITE, timeoutConfiguration, eventLogger);
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

    @NonNull
    @Override
    protected OperationDescription createOperationDescription() {
        return new OperationDescription(
                new OperationAttribute(OperationExtras.UUID, bluetoothGattCharacteristic.getUuid().toString()),
                new OperationAttribute(OperationExtras.DATA, BytePrinter.toPrettyFormattedHexString(data))
        );
    }

    @Nullable
    @Override
    protected String createOperationResultDescription(byte[] result) {
        return BytePrinter.toPrettyFormattedHexString(result);
    }
}
