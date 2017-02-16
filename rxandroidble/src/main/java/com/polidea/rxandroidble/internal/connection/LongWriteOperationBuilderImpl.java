package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationCharacteristicLongWrite;
import java.util.UUID;
import java.util.concurrent.Callable;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


final class LongWriteOperationBuilderImpl implements RxBleConnection.LongWriteOperationBuilder {

    @NonNull
    private final BluetoothGatt bluetoothGatt;

    @NonNull
    private final RxBleGattCallback rxBleGattCallback;

    @NonNull
    private final RxBleRadio rxBleRadio;

    @NonNull
    private RxBleConnection.WriteOperationAckStrategy writeOperationAckStrategy = new ImmediateSerializedBatchAckStrategy();

    private byte[] bytes;

    private Observable<BluetoothGattCharacteristic> writtenCharacteristicObservable;

    @NonNull
    private Callable<Integer> maxBatchSizeCallable;

    @NonNull
    private final RxBleConnection rxBleConnection;

    LongWriteOperationBuilderImpl(
            @NonNull BluetoothGatt bluetoothGatt,
            @NonNull RxBleGattCallback rxBleGattCallback,
            @NonNull RxBleRadio rxBleRadio,
            @NonNull Callable<Integer> defaultMaxBatchSizeCallable,
            @NonNull RxBleConnection rxBleConnection
    ) {
        this.bluetoothGatt = bluetoothGatt;
        this.rxBleGattCallback = rxBleGattCallback;
        this.rxBleRadio = rxBleRadio;
        this.maxBatchSizeCallable = defaultMaxBatchSizeCallable;
        this.rxBleConnection = rxBleConnection;
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
    public RxBleConnection.LongWriteOperationBuilder setCharacteristic(
            @NonNull BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        this.writtenCharacteristicObservable = Observable.just(bluetoothGattCharacteristic);
        return this;
    }

    @Override
    public RxBleConnection.LongWriteOperationBuilder setMaxBatchSize(final int maxBatchSize) {
        this.maxBatchSizeCallable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return maxBatchSize;
            }
        };
        return this;
    }

    @Override
    public RxBleConnection.LongWriteOperationBuilder setWriteOperationAckStrategy(
            RxBleConnection.WriteOperationAckStrategy writeOperationAckStrategy) {
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

        return writtenCharacteristicObservable.flatMap(new Func1<BluetoothGattCharacteristic, Observable<byte[]>>() {
            @Override
            public Observable<byte[]> call(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
                return rxBleRadio.queue(new RxBleRadioOperationCharacteristicLongWrite(
                        bluetoothGatt,
                        rxBleGattCallback,
                        bluetoothGattCharacteristic,
                        maxBatchSizeCallable,
                        writeOperationAckStrategy,
                        bytes,
                        AndroidSchedulers.mainThread(),
                        Schedulers.computation()
                ));
            }
        });
    }
}
