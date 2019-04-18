package com.polidea.rxandroidble2.internal.connection;

import android.bluetooth.BluetoothGattCharacteristic;
import androidx.annotation.NonNull;

import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.internal.operations.OperationsProvider;
import com.polidea.rxandroidble2.internal.serialization.ConnectionOperationQueue;

import java.util.UUID;

import javax.inject.Inject;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;

public final class LongWriteOperationBuilderImpl implements RxBleConnection.LongWriteOperationBuilder {

    private final ConnectionOperationQueue operationQueue;
    private final RxBleConnection rxBleConnection;
    private final OperationsProvider operationsProvider;

    private Single<BluetoothGattCharacteristic> writtenCharacteristicObservable;
    private PayloadSizeLimitProvider maxBatchSizeProvider;
    private RxBleConnection.WriteOperationAckStrategy writeOperationAckStrategy = new ImmediateSerializedBatchAckStrategy();
    private RxBleConnection.WriteOperationRetryStrategy writeOperationRetryStrategy = new NoRetryStrategy();

    private byte[] bytes;

    @Inject
    LongWriteOperationBuilderImpl(
            ConnectionOperationQueue operationQueue,
            MtuBasedPayloadSizeLimit defaultMaxBatchSizeProvider,
            RxBleConnection rxBleConnection,
            OperationsProvider operationsProvider
    ) {
        this.operationQueue = operationQueue;
        this.maxBatchSizeProvider = defaultMaxBatchSizeProvider;
        this.rxBleConnection = rxBleConnection;
        this.operationsProvider = operationsProvider;
    }

    @Override
    public RxBleConnection.LongWriteOperationBuilder setBytes(@NonNull byte[] bytes) {
        this.bytes = bytes;
        return this;
    }

    @Override
    public RxBleConnection.LongWriteOperationBuilder setCharacteristicUuid(@NonNull final UUID uuid) {
        this.writtenCharacteristicObservable = rxBleConnection.getCharacteristic(uuid);
        return this;
    }

    @Override
    public RxBleConnection.LongWriteOperationBuilder setCharacteristic(@NonNull BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        this.writtenCharacteristicObservable = Single.just(bluetoothGattCharacteristic);
        return this;
    }

    @Override
    public RxBleConnection.LongWriteOperationBuilder setMaxBatchSize(final int maxBatchSize) {
        this.maxBatchSizeProvider = new ConstantPayloadSizeLimit(maxBatchSize);
        return this;
    }

    @Override
    public RxBleConnection.LongWriteOperationBuilder setWriteOperationRetryStrategy(
            @NonNull RxBleConnection.WriteOperationRetryStrategy writeOperationRetryStrategy) {
        this.writeOperationRetryStrategy = writeOperationRetryStrategy;
        return this;
    }

    @Override
    public RxBleConnection.LongWriteOperationBuilder setWriteOperationAckStrategy(
            @NonNull RxBleConnection.WriteOperationAckStrategy writeOperationAckStrategy) {
        this.writeOperationAckStrategy = writeOperationAckStrategy;
        return this;
    }

    @Override
    public Observable<byte[]> build() {
        if (writtenCharacteristicObservable == null) {
            throw new IllegalArgumentException("setCharacteristicUuid() or setCharacteristic() needs to be called before build()");
        }

        if (bytes == null) {
            throw new IllegalArgumentException("setBytes() needs to be called before build()");
        }

        // TODO: [DS 24.05.2017] Think about a warning if specified maxBatchSize is greater than MTU

        return writtenCharacteristicObservable.flatMapObservable(new Function<BluetoothGattCharacteristic, Observable<byte[]>>() {
            @Override
            public Observable<byte[]> apply(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
                return operationQueue.queue(
                        operationsProvider.provideLongWriteOperation(bluetoothGattCharacteristic,
                                writeOperationAckStrategy, writeOperationRetryStrategy, maxBatchSizeProvider, bytes)
                );
            }
        });
    }
}
