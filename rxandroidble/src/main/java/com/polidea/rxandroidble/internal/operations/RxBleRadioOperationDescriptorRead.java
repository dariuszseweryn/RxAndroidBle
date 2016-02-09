package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.v4.util.Pair;
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import rx.Subscription;

public class RxBleRadioOperationDescriptorRead extends RxBleRadioOperation<Pair<BluetoothGattDescriptor, byte[]>> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final BluetoothGattDescriptor bluetoothGattDescriptor;

    public RxBleRadioOperationDescriptorRead(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                             BluetoothGattDescriptor bluetoothGattDescriptor) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.bluetoothGattDescriptor = bluetoothGattDescriptor;
    }

    @Override
    public void run() {
        //noinspection Convert2MethodRef
        final Subscription subscription = rxBleGattCallback
                .getOnDescriptorRead()
                .filter(uuidPair -> uuidPair.first.equals(bluetoothGattDescriptor))
                .first()
                .doOnCompleted(() -> releaseRadio())
                .subscribe(getSubscriber());

        final boolean success = bluetoothGatt.readDescriptor(bluetoothGattDescriptor);
        if (!success) {
            subscription.unsubscribe();
            onError(new BleGattCannotStartException(BleGattOperationType.DESCRIPTOR_READ));
        }
    }
}
