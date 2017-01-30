package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;

import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import com.polidea.rxandroidble.internal.util.ByteAssociation;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;

public class RxBleRadioOperationDescriptorRead extends RxBleRadioOperation<ByteAssociation<BluetoothGattDescriptor>> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final BluetoothGattDescriptor bluetoothGattDescriptor;

    private final Scheduler timeoutScheduler;

    public RxBleRadioOperationDescriptorRead(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                             BluetoothGattDescriptor bluetoothGattDescriptor, Scheduler timeoutScheduler) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.bluetoothGattDescriptor = bluetoothGattDescriptor;
        this.timeoutScheduler = timeoutScheduler;
    }

    @Override
    protected void protectedRun() {
        //noinspection Convert2MethodRef
        final Subscription subscription = rxBleGattCallback
                .getOnDescriptorRead()
                .filter(uuidPair -> uuidPair.first.equals(bluetoothGattDescriptor))
                .first()
                .timeout(
                        30,
                        TimeUnit.SECONDS,
                        Observable.error(new BleGattCallbackTimeoutException(bluetoothGatt, BleGattOperationType.DESCRIPTOR_READ)),
                        timeoutScheduler
                )
                .doOnCompleted(() -> releaseRadio())
                .subscribe(getSubscriber());

        final boolean success = bluetoothGatt.readDescriptor(bluetoothGattDescriptor);
        if (!success) {
            subscription.unsubscribe();
            onError(new BleGattCannotStartException(bluetoothGatt, BleGattOperationType.DESCRIPTOR_READ));
        }
    }
}
