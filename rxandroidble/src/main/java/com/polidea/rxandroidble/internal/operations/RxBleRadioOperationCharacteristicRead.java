package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleGattRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.ByteAssociation;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

public class RxBleRadioOperationCharacteristicRead extends RxBleGattRadioOperation<byte[]> {

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;

    public RxBleRadioOperationCharacteristicRead(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                                 BluetoothGattCharacteristic bluetoothGattCharacteristicObservable,
                                                 Scheduler timeoutScheduler) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.CHARACTERISTIC_READ, 30, TimeUnit.SECONDS, timeoutScheduler);
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristicObservable;
    }

    @Override
    protected Observable<byte[]> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback
                .getOnCharacteristicRead()
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
        return bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
    }
}
