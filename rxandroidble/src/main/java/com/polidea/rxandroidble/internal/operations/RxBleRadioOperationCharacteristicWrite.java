package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import java.util.concurrent.TimeUnit;
import rx.Subscription;

public class RxBleRadioOperationCharacteristicWrite extends RxBleRadioOperation<byte[]> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;

    private final byte[] data;

    private long delay;

    private TimeUnit unit;

    public RxBleRadioOperationCharacteristicWrite(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                                  BluetoothGattCharacteristic bluetoothGattCharacteristic, byte[] data) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.data = data;
    }

    public RxBleRadioOperationCharacteristicWrite(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
            BluetoothGattCharacteristic bluetoothGattCharacteristic, byte[] data, long delay, TimeUnit unit) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.data = data;
        this.delay = delay;
        this.unit = unit;
    }

    @Override
    protected void protectedRun() {
        //noinspection Convert2MethodRef
        final Subscription subscription = rxBleGattCallback
                .getOnCharacteristicWrite()
                .filter(uuidPair -> uuidPair.first.equals(bluetoothGattCharacteristic.getUuid()))
                .take(1)
                .compose(uuidPair -> (delay == 0) ? uuidPair : uuidPair.delay(delay, unit))
                .map(uuidPair -> uuidPair.second)
                .doOnCompleted(() -> releaseRadio())
                .subscribe(getSubscriber());

        bluetoothGattCharacteristic.setValue(data);
        final boolean success = bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        if (!success) {
            subscription.unsubscribe();
            onError(new BleGattCannotStartException(BleGattOperationType.CHARACTERISTIC_WRITE));
        }
    }
}
