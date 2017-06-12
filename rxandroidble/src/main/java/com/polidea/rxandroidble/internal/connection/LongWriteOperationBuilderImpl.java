package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.OperationsProvider;

import java.util.UUID;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

public final class LongWriteOperationBuilderImpl implements RxBleConnection.LongWriteOperationBuilder {

    private final RxBleRadio rxBleRadio;
    private final RxBleConnection rxBleConnection;
    private final OperationsProvider operationsProvider;

    private Observable<BluetoothGattCharacteristic> writtenCharacteristicObservable;
    private PayloadSizeLimitProvider maxBatchSizeProvider;
    private RxBleConnection.WriteOperationAckStrategy writeOperationAckStrategy = new ImmediateSerializedBatchAckStrategy();

    private byte[] bytes;

    @Inject
    LongWriteOperationBuilderImpl(
            RxBleRadio rxBleRadio,
            MtuBasedPayloadSizeLimit defaultMaxBatchSizeProvider,
            RxBleConnection rxBleConnection,
            OperationsProvider operationsProvider
    ) {
        this.rxBleRadio = rxBleRadio;
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
        this.writtenCharacteristicObservable = Observable.just(bluetoothGattCharacteristic);
        return this;
    }

    @Override
    public RxBleConnection.LongWriteOperationBuilder setMaxBatchSize(final int maxBatchSize) {
        this.maxBatchSizeProvider = new ConstantPayloadSizeLimit(maxBatchSize);
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

        return writtenCharacteristicObservable.flatMap(new Func1<BluetoothGattCharacteristic, Observable<byte[]>>() {
            @Override
            public Observable<byte[]> call(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
                return rxBleRadio.queue(
                        operationsProvider.provideLongWriteOperation(bluetoothGattCharacteristic,
                                writeOperationAckStrategy, maxBatchSizeProvider, bytes)
                );
            }
        });
    }
}
