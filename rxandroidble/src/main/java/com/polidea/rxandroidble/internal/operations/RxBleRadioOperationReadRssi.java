package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;

import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;

public class RxBleRadioOperationReadRssi extends RxBleRadioOperation<Integer> {

    private final RxBleGattCallback bleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final Scheduler timeoutScheduler;

    public RxBleRadioOperationReadRssi(RxBleGattCallback bleGattCallback, BluetoothGatt bluetoothGatt, Scheduler timeoutScheduler) {
        this.bleGattCallback = bleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.timeoutScheduler = timeoutScheduler;
    }

    @Override
    protected void protectedRun() {
        //noinspection Convert2MethodRef
        final Subscription subscription = bleGattCallback
                .getOnRssiRead()
                .take(1)
                .timeout(
                        30,
                        TimeUnit.SECONDS,
                        Observable.error(new BleGattCallbackTimeoutException(bluetoothGatt, BleGattOperationType.READ_RSSI)),
                        timeoutScheduler
                )
                .doOnCompleted(() -> releaseRadio())
                .subscribe(getSubscriber());

        final boolean success = bluetoothGatt.readRemoteRssi();
        if (!success) {
            subscription.unsubscribe();
            onError(new BleGattCannotStartException(bluetoothGatt, BleGattOperationType.READ_RSSI));
        }
    }
}
