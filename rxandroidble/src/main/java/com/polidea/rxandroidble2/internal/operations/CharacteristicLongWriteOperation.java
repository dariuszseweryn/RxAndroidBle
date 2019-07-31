package com.polidea.rxandroidble2.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.DeadObjectException;
import androidx.annotation.NonNull;

import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.LogConstants;
import com.polidea.rxandroidble2.RxBleConnection.WriteOperationAckStrategy;
import com.polidea.rxandroidble2.RxBleConnection.WriteOperationRetryStrategy;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.exceptions.BleException;
import com.polidea.rxandroidble2.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble2.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble2.exceptions.BleGattCharacteristicException;
import com.polidea.rxandroidble2.exceptions.BleGattException;
import com.polidea.rxandroidble2.exceptions.BleGattOperationType;
import com.polidea.rxandroidble2.internal.QueueOperation;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.internal.connection.ConnectionModule;
import com.polidea.rxandroidble2.internal.connection.PayloadSizeLimitProvider;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface;
import com.polidea.rxandroidble2.internal.util.ByteAssociation;
import com.polidea.rxandroidble2.internal.logger.LoggerUtil;
import com.polidea.rxandroidble2.internal.util.QueueReleasingEmitterWrapper;

import java.nio.ByteBuffer;
import java.util.UUID;

import bleshadow.javax.inject.Named;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.observers.DisposableObserver;

import static com.polidea.rxandroidble2.internal.util.DisposableUtil.disposableObserverFromEmitter;

public class CharacteristicLongWriteOperation extends QueueOperation<byte[]> {

    private final BluetoothGatt bluetoothGatt;
    private final RxBleGattCallback rxBleGattCallback;
    private final Scheduler bluetoothInteractionScheduler;
    private final TimeoutConfiguration timeoutConfiguration;
    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;
    private final PayloadSizeLimitProvider batchSizeProvider;
    private final WriteOperationAckStrategy writeOperationAckStrategy;
    private final WriteOperationRetryStrategy writeOperationRetryStrategy;
    final byte[] bytesToWrite;
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
    protected void protectedRun(final ObservableEmitter<byte[]> emitter, final QueueReleaseInterface queueReleaseInterface) {
        final int batchSize = batchSizeProvider.getPayloadSizeLimit();
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSizeProvider value must be greater than zero (now: " + batchSize + ")");
        }
        final Observable<ByteAssociation<UUID>> timeoutObservable = Observable.error(
                new BleGattCallbackTimeoutException(bluetoothGatt, BleGattOperationType.CHARACTERISTIC_LONG_WRITE)
        );
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytesToWrite);
        final QueueReleasingEmitterWrapper<byte[]> emitterWrapper = new QueueReleasingEmitterWrapper<>(emitter, queueReleaseInterface);
        final IntSupplier previousBatchIndexSupplier = new IntSupplier() {
            @Override
            public int get() {
                return (int) Math.ceil(byteBuffer.position() / (float) batchSize) - 1;
            }
        };
        writeBatchAndObserve(batchSize, byteBuffer, previousBatchIndexSupplier)
                .subscribeOn(bluetoothInteractionScheduler)
                .filter(writeResponseForMatchingCharacteristic(bluetoothGattCharacteristic))
                .take(1)
                .timeout(
                        timeoutConfiguration.timeout,
                        timeoutConfiguration.timeoutTimeUnit,
                        timeoutConfiguration.timeoutScheduler,
                        timeoutObservable
                )
                .repeatWhen(bufferIsNotEmptyAndOperationHasBeenAcknowledgedAndNotUnsubscribed(
                        writeOperationAckStrategy, byteBuffer, emitterWrapper
                ))
                .retryWhen(errorIsRetryableAndAccordingTo(writeOperationRetryStrategy, byteBuffer, batchSize, previousBatchIndexSupplier))
                .subscribe(new Observer<ByteAssociation<UUID>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        // not used
                    }

                    @Override
                    public void onNext(ByteAssociation<UUID> uuidByteAssociation) {
                        // not used
                    }

                    @Override
                    public void onError(Throwable e) {
                        emitterWrapper.onError(e);
                    }

                    @Override
                    public void onComplete() {
                        emitterWrapper.onNext(bytesToWrite);
                        emitterWrapper.onComplete();
                    }
                });
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleDisconnectedException(deadObjectException, bluetoothGatt.getDevice().getAddress(),
                BleDisconnectedException.UNKNOWN_STATUS);
    }

    @NonNull
    private Observable<ByteAssociation<UUID>> writeBatchAndObserve(final int batchSize, final ByteBuffer byteBuffer,
                                                                   final IntSupplier previousBatchIndexSupplier) {
        final Observable<ByteAssociation<UUID>> onCharacteristicWrite = rxBleGattCallback.getOnCharacteristicWrite();
        return Observable.create(
                new ObservableOnSubscribe<ByteAssociation<UUID>>() {
                    @Override
                    public void subscribe(ObservableEmitter<ByteAssociation<UUID>> emitter) {
                        final DisposableObserver writeCallbackObserver = onCharacteristicWrite
                                .subscribeWith(disposableObserverFromEmitter(emitter));
                        emitter.setDisposable(writeCallbackObserver);

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
                            writeData(bytesBatch, previousBatchIndexSupplier);
                        } catch (Throwable throwable) {
                            emitter.onError(throwable);
                        }
                    }
                });
    }

    byte[] getNextBatch(ByteBuffer byteBuffer, int batchSize) {
        final int remainingBytes = byteBuffer.remaining();
        final int nextBatchSize = Math.min(remainingBytes, batchSize);
        if (tempBatchArray == null || tempBatchArray.length != nextBatchSize) {
            tempBatchArray = new byte[nextBatchSize];
        }
        byteBuffer.get(tempBatchArray);
        return tempBatchArray;
    }

    void writeData(byte[] bytesBatch, IntSupplier batchIndexGetter) {
        if (RxBleLog.isAtLeast(LogConstants.DEBUG)) {
            RxBleLog.d("Writing batch #%04d: %s", batchIndexGetter.get(), LoggerUtil.bytesToHex(bytesBatch));
        }
        bluetoothGattCharacteristic.setValue(bytesBatch);
        final boolean success = bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        if (!success) {
            throw new BleGattCannotStartException(bluetoothGatt, BleGattOperationType.CHARACTERISTIC_LONG_WRITE);
        }
    }

    private static Predicate<ByteAssociation<UUID>> writeResponseForMatchingCharacteristic(
            final BluetoothGattCharacteristic bluetoothGattCharacteristic
    ) {
        return new Predicate<ByteAssociation<UUID>>() {
            @Override
            public boolean test(ByteAssociation<UUID> uuidByteAssociation) {
                return uuidByteAssociation.first.equals(bluetoothGattCharacteristic.getUuid());
            }
        };
    }

    static Function<Observable<?>, ObservableSource<?>> bufferIsNotEmptyAndOperationHasBeenAcknowledgedAndNotUnsubscribed(
            final WriteOperationAckStrategy writeOperationAckStrategy,
            final ByteBuffer byteBuffer,
            final QueueReleasingEmitterWrapper<byte[]> emitterWrapper) {
        return new Function<Observable<?>, ObservableSource<?>>() {

            @Override
            public ObservableSource<?> apply(Observable<?> emittingOnBatchWriteFinished) {
                return emittingOnBatchWriteFinished
                        .takeWhile(notUnsubscribed(emitterWrapper))
                        .map(bufferIsNotEmpty(byteBuffer))
                        .compose(writeOperationAckStrategy)
                        .takeWhile(new Predicate<Boolean>() {
                            @Override
                            public boolean test(Boolean hasRemaining) {
                                return hasRemaining;
                            }
                        });
            }

            @NonNull
            private Function<Object, Boolean> bufferIsNotEmpty(final ByteBuffer byteBuffer) {
                return new Function<Object, Boolean>() {
                    @Override
                    public Boolean apply(Object emittedFromActStrategy) {
                        return byteBuffer.hasRemaining();
                    }
                };
            }

            @NonNull
            private Predicate<Object> notUnsubscribed(final QueueReleasingEmitterWrapper<byte[]> emitterWrapper) {
                return new Predicate<Object>() {
                    @Override
                    public boolean test(Object emission) {
                        return !emitterWrapper.isWrappedEmitterUnsubscribed();
                    }
                };
            }
        };
    }

    private static Function<Observable<Throwable>, ObservableSource<?>> errorIsRetryableAndAccordingTo(
            final WriteOperationRetryStrategy writeOperationRetryStrategy,
            final ByteBuffer byteBuffer,
            final int batchSize,
            final IntSupplier previousBatchIndexSupplier) {
        return new Function<Observable<Throwable>, ObservableSource<?>>() {

            @Override
            public ObservableSource<?> apply(Observable<Throwable> emittedOnWriteFailure) {
                return emittedOnWriteFailure
                        .flatMap(toLongWriteFailureOrError())
                        .doOnNext(repositionByteBufferForRetry())
                        .compose(writeOperationRetryStrategy);
            }

            @NonNull
            private Function<Throwable, Observable<WriteOperationRetryStrategy.LongWriteFailure>> toLongWriteFailureOrError() {
                return new Function<Throwable, Observable<WriteOperationRetryStrategy.LongWriteFailure>>() {
                    @Override
                    public Observable<WriteOperationRetryStrategy.LongWriteFailure> apply(Throwable throwable) {
                        if (!(throwable instanceof BleGattCharacteristicException || throwable instanceof BleGattCannotStartException)) {
                            return Observable.error(throwable);
                        }
                        final int failedBatchIndex = previousBatchIndexSupplier.get();
                        WriteOperationRetryStrategy.LongWriteFailure longWriteFailure = new WriteOperationRetryStrategy.LongWriteFailure(
                                failedBatchIndex,
                                (BleGattException) throwable
                        );
                        return Observable.just(longWriteFailure);
                    }
                };
            }

            @NonNull
            private Consumer<WriteOperationRetryStrategy.LongWriteFailure> repositionByteBufferForRetry() {
                return new Consumer<WriteOperationRetryStrategy.LongWriteFailure>() {
                    @Override
                    public void accept(WriteOperationRetryStrategy.LongWriteFailure longWriteFailure) {
                        final int newBufferPosition = longWriteFailure.getBatchIndex() * batchSize;
                        byteBuffer.position(newBufferPosition);
                    }
                };
            }
        };
    }

    @Override
    public String toString() {
        return "CharacteristicLongWriteOperation{"
                + LoggerUtil.commonMacMessage(bluetoothGatt)
                + ", characteristic=" + LoggerUtil.wrap(bluetoothGattCharacteristic, false)
                + ", maxBatchSize=" + batchSizeProvider.getPayloadSizeLimit()
                + '}';
    }

    interface IntSupplier {

        int get();
    }
}
