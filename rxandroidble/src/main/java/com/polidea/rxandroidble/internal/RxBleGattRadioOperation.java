package com.polidea.rxandroidble.internal;


import android.bluetooth.BluetoothGatt;
import android.os.DeadObjectException;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;

public abstract class RxBleGattRadioOperation<T> extends RxBleRadioOperation<T> {

    private final BluetoothGatt bluetoothGatt;

    private final RxBleGattCallback rxBleGattCallback;

    private final BleGattOperationType operationType;

    private final long timeout;

    private final TimeUnit timeoutTimeUnit;

    private final Scheduler timeoutScheduler;

    public RxBleGattRadioOperation(
            BluetoothGatt bluetoothGatt,
            RxBleGattCallback rxBleGattCallback,
            BleGattOperationType operationType,
            long timeout,
            TimeUnit timeoutTimeUnit,
            Scheduler timeoutScheduler
    ) {
        this.bluetoothGatt = bluetoothGatt;
        this.rxBleGattCallback = rxBleGattCallback;
        this.operationType = operationType;
        this.timeout = timeout;
        this.timeoutTimeUnit = timeoutTimeUnit;
        this.timeoutScheduler = timeoutScheduler;
    }

    @Override
    final protected void protectedRun() throws Throwable {
        Subscription subscription = getCallback(rxBleGattCallback)
                .first()
                .timeout(
                        timeout,
                        timeoutTimeUnit,
                        timeoutFallbackProcedure(bluetoothGatt, rxBleGattCallback, timeoutScheduler),
                        timeoutScheduler
                )
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        RxBleGattRadioOperation.this.releaseRadio();
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
