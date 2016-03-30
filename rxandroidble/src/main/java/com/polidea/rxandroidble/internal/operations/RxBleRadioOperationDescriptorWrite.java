package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;

import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import rx.Subscription;

public class RxBleRadioOperationDescriptorWrite extends RxBleRadioOperation<byte[]> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final BluetoothGattDescriptor bluetoothGattDescriptor;

    private final byte[] data;

    public RxBleRadioOperationDescriptorWrite(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                              BluetoothGattDescriptor bluetoothGattDescriptor, byte[] data) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.bluetoothGattDescriptor = bluetoothGattDescriptor;
        this.data = data;
    }

    @Override
    public void run() {
        //noinspection Convert2MethodRef
        final Subscription subscription = rxBleGattCallback
                .getOnDescriptorWrite()
                .filter(uuidPair -> uuidPair.first.equals(bluetoothGattDescriptor))
                .first()
                .map(uuidPair -> uuidPair.second)
                .doOnCompleted(() -> releaseRadio())
                .subscribe(getSubscriber());

        bluetoothGattDescriptor.setValue(data);
        final boolean success = bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
        if (!success) {
            subscription.unsubscribe();
            onError(new BleGattCannotStartException(BleGattOperationType.DESCRIPTOR_WRITE));
        }
    }
}
