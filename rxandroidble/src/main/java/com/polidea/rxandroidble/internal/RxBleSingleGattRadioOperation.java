package com.polidea.rxandroidble.internal;


import android.bluetooth.BluetoothGatt;
import android.os.DeadObjectException;

import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.operations.TimeoutConfiguration;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;

public abstract class RxBleSingleGattRadioOperation<T> extends RxBleRadioOperation<T> {

    private final BluetoothGatt bluetoothGatt;
    private final RxBleGattCallback rxBleGattCallback;
    private final BleGattOperationType operationType;
    private final TimeoutConfiguration timeoutConfiguration;

    public RxBleSingleGattRadioOperation(BluetoothGatt bluetoothGatt,
                                         RxBleGattCallback rxBleGattCallback,
                                         BleGattOperationType gattOperationType,
                                         TimeoutConfiguration timeoutConfiguration) {
        this.bluetoothGatt = bluetoothGatt;
        this.rxBleGattCallback = rxBleGattCallback;
        this.operationType = gattOperationType;
        this.timeoutConfiguration = timeoutConfiguration;
    }

    @Override
    final protected void protectedRun() throws Throwable {
        Subscription subscription = getCallback(rxBleGattCallback)
                .first()
                .timeout(
                        timeoutConfiguration.timeout,
                        timeoutConfiguration.timeoutTimeUnit,
                        timeoutFallbackProcedure(bluetoothGatt, rxBleGattCallback, timeoutConfiguration.timeoutScheduler),
                        timeoutConfiguration.timeoutScheduler
                )
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        RxBleSingleGattRadioOperation.this.releaseRadio();
                    }
                })
                .subscribe(getSubscriber());

        boolean success = startOperation(bluetoothGatt);
        if (!success) {
            subscription.unsubscribe();
            onError(new BleGattCannotStartException(bluetoothGatt, operationType));
        }
    }

    abstract protected Observable<T> getCallback(RxBleGattCallback rxBleGattCallback);

    abstract protected boolean startOperation(BluetoothGatt bluetoothGatt);

    protected Observable<T> timeoutFallbackProcedure(
            BluetoothGatt bluetoothGatt,
            RxBleGattCallback rxBleGattCallback,
            Scheduler timeoutScheduler
    ) {
        return Observable.error(new BleGattCallbackTimeoutException(this.bluetoothGatt, operationType));
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleDisconnectedException(deadObjectException, bluetoothGatt.getDevice().getAddress());
    }
}
