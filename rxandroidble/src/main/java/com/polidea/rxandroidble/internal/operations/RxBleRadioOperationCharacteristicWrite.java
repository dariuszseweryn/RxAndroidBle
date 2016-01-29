package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;

public class RxBleRadioOperationCharacteristicWrite extends RxBleRadioOperation<byte[]> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;

    private final byte[] data;

    public RxBleRadioOperationCharacteristicWrite(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                                 BluetoothGattCharacteristic bluetoothGattCharacteristic, byte[] data) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.data = data;
    }

    @Override
    public void run() {
        //noinspection Convert2MethodRef
        rxBleGattCallback
                .getOnCharacteristicWrite()
                .filter(uuidPair -> uuidPair.first.equals(bluetoothGattCharacteristic.getUuid()))
                .take(1)
                .map(uuidPair -> uuidPair.second)
                .doOnCompleted(() -> releaseRadio())
                .subscribe(getSubscriber());
        // TODO: [PU] 29.01.2016 Release radio on error as well?

        bluetoothGattCharacteristic.setValue(data);
        bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
    }
}
