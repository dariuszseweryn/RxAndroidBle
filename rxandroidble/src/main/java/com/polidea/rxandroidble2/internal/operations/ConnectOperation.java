package com.polidea.rxandroidble2.internal.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.DeadObjectException;
import androidx.annotation.NonNull;

import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.exceptions.BleException;
import com.polidea.rxandroidble2.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble2.exceptions.BleGattOperationType;
import com.polidea.rxandroidble2.internal.QueueOperation;
import com.polidea.rxandroidble2.internal.connection.BluetoothGattProvider;
import com.polidea.rxandroidble2.internal.connection.ConnectionStateChangeListener;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble2.internal.logger.LoggerUtil;
import com.polidea.rxandroidble2.internal.serialization.QueueReleaseInterface;
import com.polidea.rxandroidble2.internal.util.BleConnectionCompat;

import java.util.concurrent.Callable;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.core.SingleTransformer;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;

import static com.polidea.rxandroidble2.RxBleConnection.RxBleConnectionState.CONNECTED;
import static com.polidea.rxandroidble2.RxBleConnection.RxBleConnectionState.CONNECTING;
import static com.polidea.rxandroidble2.internal.DeviceModule.CONNECT_TIMEOUT;
import static com.polidea.rxandroidble2.internal.connection.ConnectionComponent.NamedBooleans.AUTO_CONNECT;
import static com.polidea.rxandroidble2.internal.util.DisposableUtil.disposableSingleObserverFromEmitter;

public class ConnectOperation extends QueueOperation<BluetoothGatt> {

    final BluetoothDevice bluetoothDevice;
    final BleConnectionCompat connectionCompat;
    final RxBleGattCallback rxBleGattCallback;
    final BluetoothGattProvider bluetoothGattProvider;
    final TimeoutConfiguration connectTimeout;
    final boolean autoConnect;
    final ConnectionStateChangeListener connectionStateChangedAction;

    @Inject
    ConnectOperation(
            BluetoothDevice bluetoothDevice,
            BleConnectionCompat connectionCompat,
            RxBleGattCallback rxBleGattCallback,
            BluetoothGattProvider bluetoothGattProvider,
            @Named(CONNECT_TIMEOUT) TimeoutConfiguration connectTimeout,
            @Named(AUTO_CONNECT) boolean autoConnect,
            ConnectionStateChangeListener connectionStateChangedAction) {
        this.bluetoothDevice = bluetoothDevice;
        this.connectionCompat = connectionCompat;
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGattProvider = bluetoothGattProvider;
        this.connectTimeout = connectTimeout;
        this.autoConnect = autoConnect;
        this.connectionStateChangedAction = connectionStateChangedAction;
    }

    @Override
    protected void protectedRun(final ObservableEmitter<BluetoothGatt> emitter, final QueueReleaseInterface queueReleaseInterface) {
        final Action queueReleaseAction = new Action() {
            @Override
            public void run() {
                queueReleaseInterface.release();
            }
        };
        final DisposableSingleObserver<BluetoothGatt> disposableGattObserver = getConnectedBluetoothGatt()
                .compose(wrapWithTimeoutWhenNotAutoconnecting())
                // when there are no subscribers there is no point of continuing work -> next will be disconnect operation
                .doFinally(queueReleaseAction)
                .subscribeWith(disposableSingleObserverFromEmitter(emitter));
        emitter.setDisposable(disposableGattObserver);

        if (autoConnect) {
            // with autoConnect the connection may be established after a really long time
            queueReleaseInterface.release();
        }
    }

    private SingleTransformer<BluetoothGatt, BluetoothGatt> wrapWithTimeoutWhenNotAutoconnecting() {
        return new SingleTransformer<BluetoothGatt, BluetoothGatt>() {
            @Override
            public Single<BluetoothGatt> apply(Single<BluetoothGatt> bluetoothGattSingle) {
                return autoConnect
                        ? bluetoothGattSingle
                        : bluetoothGattSingle
                        .timeout(connectTimeout.timeout, connectTimeout.timeoutTimeUnit, connectTimeout.timeoutScheduler,
                                prepareConnectionTimeoutError());
            }
        };
    }

    @NonNull
    Single<BluetoothGatt> prepareConnectionTimeoutError() {
        return Single.fromCallable(new Callable<BluetoothGatt>() {
            @Override
            public BluetoothGatt call() {
                throw new BleGattCallbackTimeoutException(bluetoothGattProvider.getBluetoothGatt(), BleGattOperationType.CONNECTION_STATE);
            }
        });
    }

    /**
     * Emits BluetoothGatt and completes after connection is established.
     *
     * @return BluetoothGatt after connection reaches {@link com.polidea.rxandroidble2.RxBleConnection.RxBleConnectionState#CONNECTED}
     * state.
     * @throws com.polidea.rxandroidble2.exceptions.BleDisconnectedException if connection was disconnected/failed before
     *                                                                       it was established.
     */
    @NonNull
    private Single<BluetoothGatt> getConnectedBluetoothGatt() {
        // start connecting the BluetoothGatt
        // note: Due to different Android BLE stack implementations it is not certain whether `connectGatt()` or `BluetoothGattCallback`
        // will emit BluetoothGatt first
        return Single.create(new SingleOnSubscribe<BluetoothGatt>() {

            @Override
            public void subscribe(final SingleEmitter<BluetoothGatt> emitter) {
                final DisposableSingleObserver<BluetoothGatt> disposableGattObserver = getBluetoothGattAndChangeStatusToConnected()
                        // when the connected state will be emitted bluetoothGattProvider should contain valid Gatt
                        .delaySubscription(
                                rxBleGattCallback
                                        .getOnConnectionStateChange()
                                        .filter(new Predicate<RxBleConnection.RxBleConnectionState>() {
                                            @Override
                                            public boolean test(RxBleConnection.RxBleConnectionState rxBleConnectionState) {
                                                return rxBleConnectionState == CONNECTED;
                                            }
                                        })
                        )
                        // disconnect may happen even if the connection was not established yet
                        .mergeWith(rxBleGattCallback.<BluetoothGatt>observeDisconnect().firstOrError())
                        .firstOrError()
                        .subscribeWith(disposableSingleObserverFromEmitter(emitter));

                emitter.setDisposable(disposableGattObserver);
                connectionStateChangedAction.onConnectionStateChange(CONNECTING);

                        /*
                        * Apparently the connection may be established fast enough to introduce a race condition so the subscription
                        * must be established first before starting the connection.
                        * https://github.com/Polidea/RxAndroidBle/issues/178
                        * */

                final BluetoothGatt bluetoothGatt = connectionCompat
                        .connectGatt(bluetoothDevice, autoConnect, rxBleGattCallback.getBluetoothGattCallback());
                        /*
                        * Update BluetoothGatt when connection is initiated. It is not certain
                        * if this or RxBleGattCallback.onConnectionStateChange will be first.
                        * */
                bluetoothGattProvider.updateBluetoothGatt(bluetoothGatt);
            }
        });
    }

    Single<BluetoothGatt> getBluetoothGattAndChangeStatusToConnected() {
        return Single.fromCallable(
                new Callable<BluetoothGatt>() {
                    @Override
                    public BluetoothGatt call() {
                        connectionStateChangedAction.onConnectionStateChange(CONNECTED);
                        return bluetoothGattProvider.getBluetoothGatt();
                    }
                });
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleDisconnectedException(deadObjectException, bluetoothDevice.getAddress(), BleDisconnectedException.UNKNOWN_STATUS);
    }

    @Override
    public String toString() {
        return "ConnectOperation{"
                + LoggerUtil.commonMacMessage(bluetoothDevice.getAddress())
                + ", autoConnect=" + autoConnect
                + '}';
    }
}
