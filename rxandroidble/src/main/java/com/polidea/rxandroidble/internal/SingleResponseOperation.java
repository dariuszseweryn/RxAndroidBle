package com.polidea.rxandroidble.internal;


import android.bluetooth.BluetoothGatt;
import android.os.DeadObjectException;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.polidea.rxandroidble.internal.eventlog.OperationAttribute;
import com.polidea.rxandroidble.internal.eventlog.OperationDescription;
import com.polidea.rxandroidble.internal.eventlog.OperationEvent;
import com.polidea.rxandroidble.internal.eventlog.OperationEventLogger;
import com.polidea.rxandroidble.internal.eventlog.OperationExtras;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.operations.TimeoutConfiguration;
import com.polidea.rxandroidble.internal.serialization.QueueReleaseInterface;
import com.polidea.rxandroidble.internal.util.QueueReleasingEmitterWrapper;

import java.util.concurrent.TimeUnit;

import rx.Emitter;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;

import static com.polidea.rxandroidble.internal.eventlog.OperationEvent.operationIdentifierHash;

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
    protected final OperationEventLogger eventLogger;
    private OperationEvent operationEvent;

    public SingleResponseOperation(BluetoothGatt bluetoothGatt,
                                   RxBleGattCallback rxBleGattCallback,
                                   BleGattOperationType gattOperationType,
                                   TimeoutConfiguration timeoutConfiguration,
                                   OperationEventLogger eventLogger) {
        this.bluetoothGatt = bluetoothGatt;
        this.rxBleGattCallback = rxBleGattCallback;
        this.operationType = gattOperationType;
        this.timeoutConfiguration = timeoutConfiguration;
        this.eventLogger = eventLogger;
    }

    @Override
    @CallSuper
    public void onOperationEnqueued() {
        final String operationName = getClass().getSimpleName();
        final String deviceAddress = bluetoothGatt.getDevice().getAddress();

        if (eventLogger.isAttached()) {
            final OperationDescription operationDescription = createOperationDescription();
            operationDescription.attributes.add(new OperationAttribute(OperationExtras.TIMEOUT, timeoutConfiguration.toString()));
            operationEvent = new OperationEvent(operationIdentifierHash(this), deviceAddress, operationName, operationDescription);
            eventLogger.onOperationEnqueued(operationEvent);
        }
    }

    /**
     * Prepare operation description to be used in the log.
     *
     * @return Return a description object that will describe various parameters related with the operation, ie. timeout values, flags, etc.
     * This will be used for logging.
     */
    @NonNull
    protected OperationDescription createOperationDescription() {
        return new OperationDescription();
    }

    /**
     * Prepare a user readable description of the operation result. Keep in mind that this method will be executed only if the logging is
     * enabled. Default implementation will use a toString method.
     *
     * @param result Result of the operation
     * @return String representation of the result
     */
    @Nullable
    protected String createOperationResultDescription(T result) {
        return result.toString();
    }

    @Override
    final protected void protectedRun(final Emitter<T> emitter, final QueueReleaseInterface queueReleaseInterface) throws Throwable {
        final QueueReleasingEmitterWrapper<T> emitterWrapper = new QueueReleasingEmitterWrapper<>(emitter, queueReleaseInterface);
        Subscription subscription = getCallback(rxBleGattCallback)
                .first()
                .timeout(
                        timeoutConfiguration.timeout,
                        timeoutConfiguration.timeoutTimeUnit,
                        timeoutFallbackProcedure(bluetoothGatt, rxBleGattCallback, timeoutConfiguration.timeoutScheduler),
                        timeoutConfiguration.timeoutScheduler
                )
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        logOperationError(throwable);
                    }
                })
                .doOnNext(new Action1<T>() {
                    @Override
                    public void call(T operationResult) {
                        logOperationSuccess(operationResult);
                    }
                })
                .subscribe(emitterWrapper);

        if (!startOperation(bluetoothGatt)) {
            subscription.unsubscribe();
            final BleGattCannotStartException cannotStartException = new BleGattCannotStartException(bluetoothGatt, operationType);
            logOperationError(cannotStartException);
            emitterWrapper.onError(cannotStartException);
        } else {
            logOperationStarted();
        }
    }

    private void logOperationStarted() {
        if (eventLogger.isAttached()) {
            eventLogger.onOperationStarted(operationEvent);
        }
    }

    private void logOperationSuccess(T operationResult) {
        if (eventLogger.isAttached()) {
            eventLogger.onOperationFinished(operationEvent, createOperationResultDescription(operationResult));
        }
    }

    private void logOperationError(Throwable throwable) {
        if (eventLogger.isAttached()) {
            eventLogger.onOperationFailed(operationEvent, throwable.toString());
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
     *
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
