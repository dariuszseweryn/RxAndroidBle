package com.polidea.rxandroidble.internal.operations;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.DeadObjectException;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.ByteAssociation;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

public class RxBleRadioOperationCharacteristicLongWrite extends RxBleRadioOperation<byte[]> {

    private final BluetoothGatt bluetoothGatt;

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;

    private final Callable<Integer> batchSizeProvider;

    private final RxBleConnection.WriteOperationAckStrategy writeOperationAckStrategy;

    private final byte[] bytesToWrite;

    private final Scheduler mainThreadScheduler;

    private final Scheduler callbackScheduler;

    private final Scheduler timeoutScheduler;

    public RxBleRadioOperationCharacteristicLongWrite(
            BluetoothGatt bluetoothGatt,
            RxBleGattCallback rxBleGattCallback,
            BluetoothGattCharacteristic bluetoothGattCharacteristic,
            Callable<Integer> batchSizeProvider,
            RxBleConnection.WriteOperationAckStrategy writeOperationAckStrategy,
            byte[] bytesToWrite,
            Scheduler mainThreadScheduler,
            Scheduler callbackScheduler,
            Scheduler timeoutScheduler
    ) {
        this.bluetoothGatt = bluetoothGatt;
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.batchSizeProvider = batchSizeProvider;
        this.writeOperationAckStrategy = writeOperationAckStrategy;
        this.bytesToWrite = bytesToWrite;
        this.mainThreadScheduler = mainThreadScheduler;
        this.callbackScheduler = callbackScheduler;
        this.timeoutScheduler = timeoutScheduler;
    }

    @Override
    protected void protectedRun() throws Throwable {

        final int batchSize = batchSizeProvider.call();
        if (batchSize <= 0) {
            onError(new IllegalArgumentException("batchSizeProvider value must be greater than zero (now: " + batchSize + ")"));
            return;
        }

        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytesToWrite);
        final Observable<ByteAssociation<UUID>> timeoutObservable = Observable.error(
                new BleGattCallbackTimeoutException(bluetoothGatt, BleGattOperationType.CHARACTERISTIC_LONG_WRITE)
        );

        rxBleGattCallback.getOnCharacteristicWrite()
                .doOnSubscribe(writeNextBatch(batchSize, byteBuffer))
                .subscribeOn(mainThreadScheduler)
                .observeOn(callbackScheduler)
                .takeFirst(writeResponseForMatchingCharacteristic())
                .timeout(
                        30,
                        TimeUnit.SECONDS,
                        timeoutObservable,
                        timeoutScheduler
                )
                .map(new Func1<ByteAssociation<UUID>, Boolean>() {
                    @Override
                    public Boolean call(ByteAssociation<UUID> uuidByteAssociation) {
                        return byteBuffer.hasRemaining();
                    }
                })
                .compose(writeOperationAckStrategy)
                .ignoreElements()
                .repeatWhen(bufferIsNotEmpty(byteBuffer))
                .toCompletable()
                .subscribe(
                        new Action0() {
                            @Override
                            public void call() {
                                onNext(bytesToWrite);
                                onCompleted();
                                releaseRadio();
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                onError(throwable);
                            }
                        }
                );
    }

    private Action0 writeNextBatch(final int batchSize, final ByteBuffer byteBuffer) {
        return new Action0() {
            @Override
            public void call() {
                final byte[] bytesBatch = getNextBatch(byteBuffer, batchSize);
                writeData(bytesBatch);
            }
        };
    }

    private byte[] getNextBatch(ByteBuffer byteBuffer, int batchSize) {
        final int remainingBytes = byteBuffer.remaining();
        final int nextBatchSize = Math.min(remainingBytes, batchSize);
        final byte[] bytesBatch = new byte[nextBatchSize];
        byteBuffer.get(bytesBatch);
        return bytesBatch;
    }

    private void writeData(byte[] bytesBatch) {
        bluetoothGattCharacteristic.setValue(bytesBatch);
        final boolean success = bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        if (!success) {
            throw new BleGattCannotStartException(bluetoothGatt, BleGattOperationType.CHARACTERISTIC_LONG_WRITE);
        }
    }

    private Func1<ByteAssociation<UUID>, Boolean> writeResponseForMatchingCharacteristic() {
        return new Func1<ByteAssociation<UUID>, Boolean>() {
            @Override
            public Boolean call(ByteAssociation<UUID> uuidByteAssociation) {
                return uuidByteAssociation.first.equals(bluetoothGattCharacteristic.getUuid());
            }
        };
    }

    private Func1<Observable<? extends Void>, Observable<?>> bufferIsNotEmpty(final ByteBuffer byteBuffer) {
        return new Func1<Observable<? extends Void>, Observable<?>>() {
            @Override
            public Observable<?> call(Observable<? extends Void> emittingOnBatchWriteFinished) {
                return emittingOnBatchWriteFinished
                        .takeWhile(onNextsIfBufferIsEmpty(byteBuffer));
            }

            @NonNull
            private Func1<Object, Boolean> onNextsIfBufferIsEmpty(final ByteBuffer byteBuffer) {
                return new Func1<Object, Boolean>() {
                    @Override
                    public Boolean call(Object emittedFromActStrategy) {
                        return byteBuffer.hasRemaining();
                    }
                };
            }
        };
    }

    @Override
    protected BleDisconnectedException provideBleDisconnectedException(DeadObjectException deadObjectException) {
        return new BleDisconnectedException(deadObjectException, bluetoothGatt.getDevice().getAddress());
    }
}
