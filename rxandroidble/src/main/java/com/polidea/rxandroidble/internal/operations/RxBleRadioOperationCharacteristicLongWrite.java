package com.polidea.rxandroidble.internal.operations;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.DeadObjectException;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.ByteAssociation;

import com.polidea.rxandroidble.internal.util.OperatorDoAfterSubscribe;
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

    private static final int SINGLE_BATCH_TIMEOUT = 30;

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
        int batchSize = getBatchSize();

        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSizeProvider value must be greater than zero (now: " + batchSize + ")");
        }
        final Observable<ByteAssociation<UUID>> timeoutObservable = Observable.error(
                new BleGattCallbackTimeoutException(bluetoothGatt, BleGattOperationType.CHARACTERISTIC_LONG_WRITE)
        );
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytesToWrite);
        rxBleGattCallback.getOnCharacteristicWrite()
                .lift(new OperatorDoAfterSubscribe<ByteAssociation<UUID>>(writeNextBatch(batchSize, byteBuffer)))
                .subscribeOn(mainThreadScheduler)
                .observeOn(callbackScheduler)
                .takeFirst(writeResponseForMatchingCharacteristic())
                .timeout(
                        SINGLE_BATCH_TIMEOUT,
                        TimeUnit.SECONDS,
                        timeoutObservable,
                        timeoutScheduler
                )
                .repeatWhen(bufferIsNotEmptyAndOperationHasBeenAcknowledged(byteBuffer))
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

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleDisconnectedException(deadObjectException, bluetoothGatt.getDevice().getAddress());
    }

    private int getBatchSize() {
        try {
            return batchSizeProvider.call();
        } catch (Exception e) {
            RxBleLog.w(e, "Failed to get batch size.");
            throw new RuntimeException("Failed to get batch size from the batchSizeProvider.", e);
        }
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

    private Func1<Observable<? extends Void>, Observable<?>> bufferIsNotEmptyAndOperationHasBeenAcknowledged(final ByteBuffer byteBuffer) {
        return new Func1<Observable<? extends Void>, Observable<?>>() {
            @Override
            public Observable<?> call(Observable<? extends Void> emittingOnBatchWriteFinished) {
                return writeOperationAckStrategy.call(emittingOnBatchWriteFinished.map(bufferIsNotEmpty(byteBuffer)))
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
        };
    }
}
