package com.polidea.rxandroidble.internal.operations;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.DeadObjectException;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.RxBleConnection.WriteOperationAckStrategy;
import com.polidea.rxandroidble.RxBleConnection.WriteOperationRetryStrategy;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattCharacteristicException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.QueueOperation;
import com.polidea.rxandroidble.internal.connection.ConnectionModule;
import com.polidea.rxandroidble.internal.connection.PayloadSizeLimitProvider;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.serialization.QueueReleaseInterface;
import com.polidea.rxandroidble.internal.util.ByteAssociation;
import com.polidea.rxandroidble.internal.util.QueueReleasingEmitterWrapper;

import java.nio.ByteBuffer;
import java.util.UUID;

import bleshadow.javax.inject.Named;
import rx.Emitter;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

public class CharacteristicLongWriteOperation extends QueueOperation<byte[]> {

    private final BluetoothGatt bluetoothGatt;
    private final RxBleGattCallback rxBleGattCallback;
    private final Scheduler bluetoothInteractionScheduler;
    private final TimeoutConfiguration timeoutConfiguration;
    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private final PayloadSizeLimitProvider batchSizeProvider;
    private final WriteOperationAckStrategy writeOperationAckStrategy;
    private final WriteOperationRetryStrategy writeOperationRetryStrategy;
    private final byte[] bytesToWrite;
    private byte[] tempBatchArray;

    CharacteristicLongWriteOperation(
            BluetoothGatt bluetoothGatt,
            RxBleGattCallback rxBleGattCallback,
            @Named(ClientComponent.NamedSchedulers.BLUETOOTH_INTERACTION) Scheduler bluetoothInteractionScheduler,
            @Named(ConnectionModule.OPERATION_TIMEOUT) TimeoutConfiguration timeoutConfiguration,
            BluetoothGattCharacteristic bluetoothGattCharacteristic,
            PayloadSizeLimitProvider batchSizeProvider,
            WriteOperationAckStrategy writeOperationAckStrategy,
            WriteOperationRetryStrategy writeOperationRetryStrategy,
            byte[] bytesToWrite) {
        this.bluetoothGatt = bluetoothGatt;
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothInteractionScheduler = bluetoothInteractionScheduler;
        this.timeoutConfiguration = timeoutConfiguration;
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.batchSizeProvider = batchSizeProvider;
        this.writeOperationAckStrategy = writeOperationAckStrategy;
        this.writeOperationRetryStrategy = writeOperationRetryStrategy;
        this.bytesToWrite = bytesToWrite;
    }

    @Override
    protected void protectedRun(final Emitter<byte[]> emitter, final QueueReleaseInterface queueReleaseInterface) throws Throwable {
        final int batchSize = batchSizeProvider.getPayloadSizeLimit();

        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSizeProvider value must be greater than zero (now: " + batchSize + ")");
        }
        final Observable<ByteAssociation<UUID>> timeoutObservable = Observable.error(
                new BleGattCallbackTimeoutException(bluetoothGatt, BleGattOperationType.CHARACTERISTIC_LONG_WRITE)
        );
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytesToWrite);

        final QueueReleasingEmitterWrapper<byte[]> emitterWrapper = new QueueReleasingEmitterWrapper<>(emitter, queueReleaseInterface);
        writeBatchAndObserve(batchSize, byteBuffer)
                .subscribeOn(bluetoothInteractionScheduler)
                .takeFirst(writeResponseForMatchingCharacteristic(bluetoothGattCharacteristic))
                .timeout(
                        timeoutConfiguration.timeout,
                        timeoutConfiguration.timeoutTimeUnit,
                        timeoutObservable,
                        timeoutConfiguration.timeoutScheduler
                )
                .repeatWhen(bufferIsNotEmptyAndOperationHasBeenAcknowledgedAndNotUnsubscribed(
                        writeOperationAckStrategy, byteBuffer, emitterWrapper
                ))
                .retryWhen(errorIsRetryableAndAccordingTo(writeOperationRetryStrategy, byteBuffer, batchSize))
                .toCompletable()
                .subscribe(
                        new Action0() {
                            @Override
                            public void call() {
                                emitterWrapper.onNext(bytesToWrite);
                                emitterWrapper.onCompleted();
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                emitterWrapper.onError(throwable);
                            }
                        }
                );
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleDisconnectedException(deadObjectException, bluetoothGatt.getDevice().getAddress());
    }

    @NonNull
    private Observable<ByteAssociation<UUID>> writeBatchAndObserve(final int batchSize, final ByteBuffer byteBuffer) {
        final Observable<ByteAssociation<UUID>> onCharacteristicWrite = rxBleGattCallback.getOnCharacteristicWrite();
        return Observable.create(
                new Action1<Emitter<ByteAssociation<UUID>>>() {
                    @Override
                    public void call(Emitter<ByteAssociation<UUID>> emitter) {
                        final Subscription s = onCharacteristicWrite.subscribe(emitter);
                        emitter.setSubscription(s);

                        /*
                         * Since Android OS calls {@link android.bluetooth.BluetoothGattCallback} callbacks on arbitrary background
                         * threads - in case the {@link BluetoothGattCharacteristic} has
                         * a {@link BluetoothGattCharacteristic#WRITE_TYPE_NO_RESPONSE} set it is possible that
                         * a {@link android.bluetooth.BluetoothGattCallback#onCharacteristicWrite} may be called before the
                         * {@link BluetoothGatt#writeCharacteristic(BluetoothGattCharacteristic)} will return.
                         * Because of such a situation - it is important to first establish a full RxJava flow and only then
                         * call writeCharacteristic.
                         */

                        try {
                            final byte[] bytesBatch = getNextBatch(byteBuffer, batchSize);
                            writeData(bytesBatch);
                        } catch (Throwable throwable) {
                            emitter.onError(throwable);
                        }
                    }
                },
                Emitter.BackpressureMode.BUFFER);
    }

    private byte[] getNextBatch(ByteBuffer byteBuffer, int batchSize) {
        final int remainingBytes = byteBuffer.remaining();
        final int nextBatchSize = Math.min(remainingBytes, batchSize);
        if (tempBatchArray == null || tempBatchArray.length != nextBatchSize) {
            tempBatchArray = new byte[nextBatchSize];
        }
        byteBuffer.get(tempBatchArray);
        return tempBatchArray;
    }

    private void writeData(byte[] bytesBatch) {
        bluetoothGattCharacteristic.setValue(bytesBatch);
        final boolean success = bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        if (!success) {
            throw new BleGattCannotStartException(bluetoothGatt, BleGattOperationType.CHARACTERISTIC_LONG_WRITE);
        }
    }

    private static Func1<ByteAssociation<UUID>, Boolean> writeResponseForMatchingCharacteristic(
            final BluetoothGattCharacteristic bluetoothGattCharacteristic
    ) {
        return new Func1<ByteAssociation<UUID>, Boolean>() {
            @Override
            public Boolean call(ByteAssociation<UUID> uuidByteAssociation) {
                return uuidByteAssociation.first.equals(bluetoothGattCharacteristic.getUuid());
            }
        };
    }

    private static Func1<Observable<? extends Void>, Observable<?>> bufferIsNotEmptyAndOperationHasBeenAcknowledgedAndNotUnsubscribed(
            final WriteOperationAckStrategy writeOperationAckStrategy,
            final ByteBuffer byteBuffer,
            final QueueReleasingEmitterWrapper<byte[]> emitterWrapper) {
        return new Func1<Observable<? extends Void>, Observable<?>>() {
            @Override
            public Observable<?> call(Observable<? extends Void> emittingOnBatchWriteFinished) {
                return writeOperationAckStrategy.call(
                        emittingOnBatchWriteFinished
                                .takeWhile(notUnsubscribed(emitterWrapper))
                                .map(bufferIsNotEmpty(byteBuffer))
                )
                        .takeWhile(bufferIsNotEmpty(byteBuffer));
            }

            @NonNull
            private Func1<Object, Boolean> bufferIsNotEmpty(final ByteBuffer byteBuffer) {
                return new Func1<Object, Boolean>() {
                    @Override
                    public Boolean call(Object emittedFromActStrategy) {
                        return byteBuffer.hasRemaining();
                    }
                };
            }

            @NonNull
            private Func1<Object, Boolean> notUnsubscribed(final QueueReleasingEmitterWrapper<byte[]> emitterWrapper) {
                return new Func1<Object, Boolean>() {
                    @Override
                    public Boolean call(Object emission) {
                        return !emitterWrapper.isWrappedEmitterUnsubscribed();
                    }
                };
            }
        };
    }

    private static Func1<Observable<? extends Throwable>, Observable<?>> errorIsRetryableAndAccordingTo(
            final WriteOperationRetryStrategy writeOperationRetryStrategy,
            final ByteBuffer byteBuffer,
            final int batchSize) {
        return new Func1<Observable<? extends Throwable>, Observable<?>>() {
            @Override
            public Observable<?> call(Observable<? extends Throwable> emittedOnWriteFailure) {
                return emittedOnWriteFailure
                        .flatMap(toLongWriteFailureOrError())
                        .doOnNext(repositionByteBufferForRetry())
                        .compose(writeOperationRetryStrategy);
            }

            @NonNull
            private Func1<Throwable, Observable<WriteOperationRetryStrategy.LongWriteFailure>> toLongWriteFailureOrError() {
                return new Func1<Throwable, Observable<WriteOperationRetryStrategy.LongWriteFailure>>() {
                    @Override
                    public Observable<WriteOperationRetryStrategy.LongWriteFailure> call(Throwable throwable) {
                        if (!(throwable instanceof BleGattCharacteristicException)) {
                            return Observable.error(throwable);
                        }
                        final int failedBatchNumber = calculateFailedBatchNumber(byteBuffer, batchSize);
                        WriteOperationRetryStrategy.LongWriteFailure longWriteFailure = new WriteOperationRetryStrategy.LongWriteFailure(
                                failedBatchNumber,
                                (BleGattCharacteristicException) throwable
                        );
                        return Observable.just(longWriteFailure);
                    }
                };
            }

            @NonNull
            private Action1<WriteOperationRetryStrategy.LongWriteFailure> repositionByteBufferForRetry() {
                return new Action1<WriteOperationRetryStrategy.LongWriteFailure>() {
                    @Override
                    public void call(WriteOperationRetryStrategy.LongWriteFailure longWriteFailure) {
                        final int newBufferPosition = longWriteFailure.getBatchNumber() * batchSize;
                        byteBuffer.position(newBufferPosition);
                    }
                };
            }

            private int calculateFailedBatchNumber(ByteBuffer byteBuffer, int batchSize) {
                if (byteBuffer.hasRemaining()) {
                    return (byteBuffer.position() / batchSize) - 1;
                } else {
                    return byteBuffer.position() / batchSize;
                }
            }
        };
    }
}
