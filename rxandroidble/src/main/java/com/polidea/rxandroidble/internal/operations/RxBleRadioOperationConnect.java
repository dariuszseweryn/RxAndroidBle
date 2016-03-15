package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.BleConnectionCompat;

import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTED;

public class RxBleRadioOperationConnect extends RxBleRadioOperation<BluetoothGatt> {

    private final BluetoothDevice bluetoothDevice;
    private final RxBleGattCallback rxBleGattCallback;
    private final BleConnectionCompat connectionCompat;
    private final boolean autoConnect;
    private BehaviorSubject<BluetoothGatt> bluetoothGattBehaviorSubject = BehaviorSubject.create();
    private Subscription bluetoothGattSubscription;

    public RxBleRadioOperationConnect(BluetoothDevice bluetoothDevice, RxBleGattCallback rxBleGattCallback,
                                      BleConnectionCompat connectionCompat, boolean autoConnect) {
        this.bluetoothDevice = bluetoothDevice;
        this.rxBleGattCallback = rxBleGattCallback;
        this.connectionCompat = connectionCompat;
        this.autoConnect = autoConnect;
    }

    @Override
    public Observable<BluetoothGatt> asObservable() {
        return super.asObservable()
                .doOnUnsubscribe(() -> {
                    if (bluetoothGattSubscription != null) {
                        bluetoothGattSubscription.unsubscribe();
                    }
                });
    }

    @Override
    public void run() {
        //noinspection Convert2MethodRef
        final Runnable releaseRadioRunnable = () -> releaseRadio();
        final Runnable emptyRunnable = () -> {
        };

        final Runnable onNextRunnable = autoConnect ? emptyRunnable : releaseRadioRunnable;
        final Runnable onConnectCalledRunnable = autoConnect ? releaseRadioRunnable : emptyRunnable;

        //noinspection Convert2MethodRef
        getConnectedBluetoothGattObservable()
                .subscribe(
                        bluetoothGatt -> {
                            onNext(bluetoothGatt);
                            onNextRunnable.run();
                        },
                        (throwable) -> onError(throwable),
                        () -> onCompleted()
                );

        bluetoothGattSubscription = rxBleGattCallback.getBluetoothGatt()
                .doOnUnsubscribe(bluetoothGattBehaviorSubject::onCompleted)
                .subscribe(
                        bluetoothGattBehaviorSubject::onNext,
                        ignored -> bluetoothGattBehaviorSubject.onCompleted(),
                        bluetoothGattBehaviorSubject::onCompleted
                );

        bluetoothGattBehaviorSubject.onNext(connectionCompat.connectGatt(bluetoothDevice, autoConnect,
                rxBleGattCallback.getBluetoothGattCallback()));
        onConnectCalledRunnable.run();
    }

    @NonNull
    private Observable<BluetoothGatt> getConnectedBluetoothGattObservable() {
        return Observable.combineLatest(
                rxBleGattCallback
                        .getOnConnectionStateChange()
                        .filter(rxBleConnectionState -> rxBleConnectionState == CONNECTED), // waiting for connected state
                getBluetoothGatt(), // using latest BluetoothGatt
                (rxBleConnectionState, bluetoothGatt) -> bluetoothGatt
        )
                .mergeWith(rxBleGattCallback.disconnectedErrorObservable()) // if gatt will disconnect during connecting emit error
                .first(); // using first
    }

    public Observable<BluetoothGatt> getBluetoothGatt() {
        return bluetoothGattBehaviorSubject;
    }
}
