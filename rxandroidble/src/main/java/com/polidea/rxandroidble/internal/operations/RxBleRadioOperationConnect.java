package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleConnection;
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
    private final Runnable releaseRadioRunnable = () -> releaseRadio();
    private final Runnable emptyRunnable = () -> {
    };

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
                        bluetoothGattSubscription = null;
                    }
                });
    }

    @Override
    protected void protectedRun() {
        final Runnable onConnectionEstablishedRunnable = autoConnect ? emptyRunnable : releaseRadioRunnable;
        final Runnable onConnectCalledRunnable = autoConnect ? releaseRadioRunnable : emptyRunnable;
        // TODO: [PU] 22.03.2016 Is radio properly released in autoConnect in case of connection error?
        //noinspection Convert2MethodRef
        observeBluetoothGattAfterConnectionEstablished()
                .subscribe(
                        bluetoothGatt -> {
                            onNext(bluetoothGatt);
                            onConnectionEstablishedRunnable.run();
                        },
                        (throwable) -> onError(throwable),
                        () -> onCompleted()
                );

        // Listen for BluetoothGatt instance updates and complete the subject if there won't be more updates.
        bluetoothGattSubscription = rxBleGattCallback.getBluetoothGatt()
                .startWith(connect())
                .doOnUnsubscribe(bluetoothGattBehaviorSubject::onCompleted)
                .subscribe(this::postUpdatedBluetoothGatt);
        onConnectCalledRunnable.run();
    }

    private void postUpdatedBluetoothGatt(BluetoothGatt bluetoothGatt) {
        bluetoothGattBehaviorSubject.onNext(bluetoothGatt);
    }

    private BluetoothGatt connect() {
        return connectionCompat.connectGatt(bluetoothDevice, autoConnect, rxBleGattCallback.getBluetoothGattCallback());
    }

    /**
     * Emits BluetoothGatt and completes after connection is established.
     *
     * @return BluetoothGatt after connection reaches {@link RxBleConnection.RxBleConnectionState#CONNECTED} state.
     * @throws com.polidea.rxandroidble.exceptions.BleDisconnectedException if connection was disconnected/failed before it was established.
     */
    @NonNull
    private Observable<BluetoothGatt> observeBluetoothGattAfterConnectionEstablished() {
        return Observable.combineLatest(
                observeConnectionEstablishedEvents(), // waiting for connected state.
                bluetoothGattBehaviorSubject, // using latest BluetoothGatt
                (rxBleConnectionState, bluetoothGatt) -> bluetoothGatt // only BluetoothGatt is useful for us.
        )
                .mergeWith(rxBleGattCallback.observeDisconnect()) // disconnect may happen even if the connection was not established yet.
                .first();
    }

    @NonNull
    private Observable<RxBleConnection.RxBleConnectionState> observeConnectionEstablishedEvents() {
        return rxBleGattCallback
                .getOnConnectionStateChange()
                .filter(rxBleConnectionState -> rxBleConnectionState == CONNECTED);
    }

    /**
     * Obtain observable emitting most recent {@link BluetoothGatt instance}.
     * NOTE: Connection may be released and/or GATT may be closed in any point of time.
     *
     * @return Observable with BluetoothGatt. Most recent GATT will be emitted instantly after subscription if it is available.
     */
    public Observable<BluetoothGatt> getBluetoothGatt() {
        return bluetoothGattBehaviorSubject;
    }
}
