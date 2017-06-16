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

import com.polidea.rxandroidble.internal.util.RadioReleasingEmitterWrapper;
import java.util.concurrent.TimeUnit;
import rx.Emitter;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;

/**
 * A convenience class intended to use with {@link BluetoothGatt} functions that fire one-time actions.
 * @param <T> The type of emitted result.
 */
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
    final protected void protectedRun(final Emitter<T> emitter, final RadioReleaseInterface radioReleaseInterface) throws Throwable {
        final RadioReleasingEmitterWrapper<T> emitterWrapper = new RadioReleasingEmitterWrapper<>(emitter, radioReleaseInterface);
        Subscription subscription = getCallback(rxBleGattCallback)
                .first()
                .timeout(
                        timeoutConfiguration.timeout,
                        timeoutConfiguration.timeoutTimeUnit,
                        timeoutFallbackProcedure(bluetoothGatt, rxBleGattCallback, timeoutConfiguration.timeoutScheduler),
                        timeoutConfiguration.timeoutScheduler
                )
                .subscribe(emitterWrapper);

        if (!startOperation(bluetoothGatt)) {
            subscription.unsubscribe();
            emitterWrapper.onError(new BleGattCannotStartException(bluetoothGatt, operationType));
        }
    }

    /**
     * A function that should return {@link Observable} derived from the passed {@link RxBleGattCallback}.
     * The returned {@link Observable} will be automatically unsubscribed after the first emission.
     * The returned {@link Observable} is a subject to {@link Observable#timeout(long, TimeUnit, Observable, Scheduler)} and by default
     * it will throw {@link BleGattCallbackTimeoutException}. This behaviour can be overriden by overriding
     * {@link #timeoutFallbackProcedure(BluetoothGatt, RxBleGattCallback, Scheduler)}.
     *
     * @param rxBleGattCallback the {@link RxBleGattCallback} to use
     * @return the Observable
     */
    abstract protected Observable<T> getCallback(RxBleGattCallback rxBleGattCallback);

    /**
     * A function that should call the passed {@link BluetoothGatt} and return `true` if the call has succeeded.
     * @param bluetoothGatt the {@link BluetoothGatt} to use
     * @return `true` if success, `false` otherwise
     */
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
