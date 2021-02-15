package com.polidea.rxandroidble2.internal;

import android.bluetooth.BluetoothGatt;
import android.os.DeadObjectException;

import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.exceptions.BleException;
import com.polidea.rxandroidble2.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble2.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble2.exceptions.BleGattOperationType;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble2.internal.logger.LoggerUtil;
import com.polidea.rxandroidble2.internal.operations.TimeoutConfiguration;
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface;
import com.polidea.rxandroidble2.internal.util.QueueReleasingEmitterWrapper;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;

/**
 * A convenience class intended to use with {@link BluetoothGatt} functions that fire one-time actions.
 *
 * @param <T> The type of emitted result.
 */
public abstract class SingleResponseOperation<T> extends QueueOperation<T> {

    private final BluetoothGatt bluetoothGatt;
    private final RxBleGattCallback rxBleGattCallback;
    private final BleGattOperationType operationType;
    private final TimeoutConfiguration timeoutConfiguration;

    public SingleResponseOperation(BluetoothGatt bluetoothGatt,
                                   RxBleGattCallback rxBleGattCallback,
                                   BleGattOperationType gattOperationType,
                                   TimeoutConfiguration timeoutConfiguration) {
        this.bluetoothGatt = bluetoothGatt;
        this.rxBleGattCallback = rxBleGattCallback;
        this.operationType = gattOperationType;
        this.timeoutConfiguration = timeoutConfiguration;
    }

    @Override
    final protected void protectedRun(final ObservableEmitter<T> emitter, final QueueReleaseInterface queueReleaseInterface) {
        final QueueReleasingEmitterWrapper<T> emitterWrapper = new QueueReleasingEmitterWrapper<>(emitter, queueReleaseInterface);
        getCallback(rxBleGattCallback)
                .timeout(
                        timeoutConfiguration.timeout,
                        timeoutConfiguration.timeoutTimeUnit,
                        timeoutConfiguration.timeoutScheduler,
                        timeoutFallbackProcedure(bluetoothGatt, rxBleGattCallback, timeoutConfiguration.timeoutScheduler)
                )
                .toObservable()
                .subscribe(emitterWrapper);

        if (!startOperation(bluetoothGatt)) {
            emitterWrapper.cancel();
            emitterWrapper.onError(new BleGattCannotStartException(bluetoothGatt, operationType));
        }
    }

    /**
     * A function that should return {@link Observable} derived from the passed {@link RxBleGattCallback}.
     * The returned {@link Observable} will be automatically unsubscribed after the first emission.
     * The returned {@link Observable} is a subject to
     * {@link Observable#timeout(long, TimeUnit, Scheduler, io.reactivex.rxjava3.core.ObservableSource)}
     * and by default it will throw {@link BleGattCallbackTimeoutException}. This behaviour can be overridden by overriding
     * {@link #timeoutFallbackProcedure(BluetoothGatt, RxBleGattCallback, Scheduler)}.
     *
     * @param rxBleGattCallback the {@link RxBleGattCallback} to use
     * @return the Observable
     */
    abstract protected Single<T> getCallback(RxBleGattCallback rxBleGattCallback);

    /**
     * A function that should call the passed {@link BluetoothGatt} and return `true` if the call has succeeded.
     *
     * @param bluetoothGatt the {@link BluetoothGatt} to use
     * @return `true` if success, `false` otherwise
     */
    abstract protected boolean startOperation(BluetoothGatt bluetoothGatt);

    @SuppressWarnings("unused")
    protected Single<T> timeoutFallbackProcedure(
            BluetoothGatt bluetoothGatt,
            RxBleGattCallback rxBleGattCallback,
            Scheduler timeoutScheduler
    ) {
        return Single.error(new BleGattCallbackTimeoutException(this.bluetoothGatt, operationType));
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleDisconnectedException(deadObjectException, bluetoothGatt.getDevice().getAddress(),
                BleDisconnectedException.UNKNOWN_STATUS);
    }

    @Override
    public String toString() {
        return LoggerUtil.commonMacMessage(bluetoothGatt);
    }
}
