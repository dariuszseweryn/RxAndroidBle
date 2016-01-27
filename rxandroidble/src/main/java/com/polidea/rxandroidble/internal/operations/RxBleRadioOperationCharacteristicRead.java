package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.support.v4.util.Pair;
import android.util.Log;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import rx.Observable;

public class RxBleRadioOperationCharacteristicRead extends RxBleRadioOperation<byte[]> {

    private final String TAG = getClass().getSimpleName() + '(' + System.identityHashCode(this) + ')';

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final Observable<BluetoothGattCharacteristic> bluetoothGattCharacteristicObservable;

    public RxBleRadioOperationCharacteristicRead(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                                 Observable<BluetoothGattCharacteristic> bluetoothGattCharacteristicObservable) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.bluetoothGattCharacteristicObservable = bluetoothGattCharacteristicObservable;
    }

    @Override
    public void run() {
        bluetoothGattCharacteristicObservable
                .doOnNext(anything -> Log.d(TAG, "run 1"))
                .map(bluetoothGattCharacteristic -> {
                    Log.d(TAG, "Got characteristic " + bluetoothGattCharacteristic.getUuid().toString());
                    return new Pair<>(
                            rxBleGattCallback
                                    .getOnCharacteristicRead()
                                    .doOnNext(bytes -> Log.d(TAG, "Read ."))
                                    .filter(uuidPair -> uuidPair.first.equals(bluetoothGattCharacteristic.getUuid()))
                                    .doOnNext(bytes -> Log.d(TAG, "Read .."))
                                    .first()
                                    .map(uuidPair1 -> uuidPair1.second)
                                    .doOnSubscribe(() -> Log.d(TAG, "rxBleGattCallback subscribed"))
                                    .doOnUnsubscribe(() -> {
                                        Log.d(TAG, "rxBleGattCallback unsubscribed");
                                    })
                                    .doOnCompleted(() -> Log.d(TAG, "rxBleGattCallback completed"))
                                    .doOnError(throwable -> Log.d(TAG, "rxBleGattCallback errored"))
                                    .doOnNext(bytes -> Log.d(TAG, "Read ... " + new String(bytes))),
                            bluetoothGattCharacteristic
                    );
                })
                .doOnNext(anything -> Log.d(TAG, "run 2"))
                .doOnNext(pair -> bluetoothGatt.readCharacteristic(pair.second))
                .flatMap(pair -> pair.first)
                .doOnNext(anything -> Log.d(TAG, "run 3"))
                .doOnNext(bytes -> releaseRadio())
                .doOnSubscribe(() -> Log.d(TAG, "subscribed"))
                .doOnUnsubscribe(() -> {
                    Log.d(TAG, "unsubscribed");
                })
                .subscribe(getSubscriber());
    }
}
