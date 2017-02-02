package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleGattRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;

public class RxBleRadioOperationReadRssi extends RxBleGattRadioOperation<Integer> {

    private final Scheduler timeoutScheduler;

    public RxBleRadioOperationReadRssi(RxBleGattCallback bleGattCallback, BluetoothGatt bluetoothGatt, Scheduler timeoutScheduler) {
        super(bluetoothGatt, bleGattCallback, BleGattOperationType.READ_RSSI);
        this.timeoutScheduler = timeoutScheduler;
    }

    @Override
    protected void protectedRun() {
        //noinspection Convert2MethodRef
        final Subscription subscription = rxBleGattCallback
                .getOnRssiRead()
                .take(1)
                .timeout(
                        30,
                        TimeUnit.SECONDS,
                        Observable.<Integer>error(newTimeoutException()),
                        timeoutScheduler
                )
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        RxBleRadioOperationReadRssi.this.releaseRadio();
                    }
                })
                .subscribe(getSubscriber());

        final boolean success = bluetoothGatt.readRemoteRssi();
        if (!success) {
            subscription.unsubscribe();
            onError(newCannotStartException());
        }
    }
}
