package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.BleConnectionCompat;

import rx.Observable;
import rx.subjects.BehaviorSubject;

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTED;

public class RxBleRadioOperationConnect extends RxBleRadioOperation<BluetoothGatt> {

    private final BluetoothDevice bluetoothDevice;
    private final RxBleGattCallback rxBleGattCallback;
    private final BleConnectionCompat connectionCompat;
    private final boolean autoConnect;
    private BehaviorSubject<BluetoothGatt> bluetoothGattBehaviorSubject = BehaviorSubject.create();
    @SuppressWarnings("Convert2MethodRef")
    private final Runnable releaseRadioRunnable = () -> releaseRadio();
    private final Runnable emptyRunnable = () -> {
    };
    private final BehaviorSubject<Boolean> isSubscribed = BehaviorSubject.create();
    private final Observable<BluetoothGatt> operationConnectAsObservableWithSubscribersMonitoring = super.asObservable()
            .doOnSubscribe(() -> isSubscribed.onNext(true))
            .doOnUnsubscribe(() -> isSubscribed.onNext(false))
            .share();

    public RxBleRadioOperationConnect(BluetoothDevice bluetoothDevice, RxBleGattCallback rxBleGattCallback,
                                      BleConnectionCompat connectionCompat, boolean autoConnect) {
        this.bluetoothDevice = bluetoothDevice;
        this.rxBleGattCallback = rxBleGattCallback;
        this.connectionCompat = connectionCompat;
        this.autoConnect = autoConnect;
    }

    @Override
    public Observable<BluetoothGatt> asObservable() {
        return operationConnectAsObservableWithSubscribersMonitoring;
    }

    @Override
    protected void protectedRun() {
        final Runnable onConnectionEstablishedRunnable = autoConnect ? emptyRunnable : releaseRadioRunnable;
        final Runnable onConnectCalledRunnable = autoConnect ? releaseRadioRunnable : emptyRunnable;

        getConnectedBluetoothGatt()
                // when there are no subscribers there is no point of continuing work -> next will be disconnect operation
                .takeUntil(asObservableHasNoSubscribers().doOnNext(noSubscribers -> RxBleLog.d("No subscribers, finishing operation")))
                .doOnCompleted(onConnectionEstablishedRunnable::run)
                .doOnNext(ignored -> isSubscribed.onCompleted())
                .subscribe(getSubscriber());
        onConnectCalledRunnable.run();
    }

    @NonNull
    private Observable<Boolean> asObservableHasNoSubscribers() {
        return isSubscribed.filter(aBoolean -> !aBoolean);
    }

    /**
     * Emits BluetoothGatt and completes after connection is established.
     *
     * @return BluetoothGatt after connection reaches {@link com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState#CONNECTED} state.
     * @throws com.polidea.rxandroidble.exceptions.BleDisconnectedException if connection was disconnected/failed before it was established.
     */
    @NonNull
    private Observable<BluetoothGatt> getConnectedBluetoothGatt() {
        // start connecting the BluetoothGatt
        // note: Due to different Android BLE stack implementations it is not certain whether `connectGatt()` or `BluetoothGattCallback`
        // will emit BluetoothGatt first
        return Observable.fromCallable(() ->
                connectionCompat.connectGatt(bluetoothDevice, autoConnect, rxBleGattCallback.getBluetoothGattCallback())
        )
                .mergeWith(rxBleGattCallback.getBluetoothGatt())
                // relay BluetoothGatt instance updates
                .doOnNext(bluetoothGattBehaviorSubject::onNext)
                // finish relaying if there won't be more updates
                .doOnTerminate(bluetoothGattBehaviorSubject::onCompleted)
                // disconnect may happen even if the connection was not established yet
                .mergeWith(rxBleGattCallback.observeDisconnect())
                // capture BluetoothGatt when connected
                .sample(rxBleGattCallback
                        .getOnConnectionStateChange()
                        .filter(rxBleConnectionState -> rxBleConnectionState == CONNECTED))
                .take(1)
                // finish relaying if there won't be more updates
                .doOnTerminate(bluetoothGattBehaviorSubject::onCompleted);
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
