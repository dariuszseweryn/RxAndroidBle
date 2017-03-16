package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.DeadObjectException;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.BluetoothGattProvider;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.BleConnectionCompat;

import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTED;
import static com.polidea.rxandroidble.internal.DeviceModule.CONNECT_TIMEOUT;

public class RxBleRadioOperationConnect extends RxBleRadioOperation<BluetoothGatt> {

    public static class Builder {

        private final BluetoothDevice bluetoothDevice;
        private final BleConnectionCompat connectionCompat;
        private final RxBleGattCallback rxBleGattCallback;
        private final BluetoothGattProvider bluetoothGattProvider;
        private final TimeoutConfiguration connectTimeout;
        private boolean autoConnect = false;

        @Inject
        public Builder(
                BluetoothDevice bluetoothDevice,
                BleConnectionCompat connectionCompat,
                RxBleGattCallback rxBleGattCallback,
                @Named(CONNECT_TIMEOUT) TimeoutConfiguration connectionTimeout,
                BluetoothGattProvider bluetoothGattProvider) {
            this.bluetoothDevice = bluetoothDevice;
            this.connectionCompat = connectionCompat;
            this.rxBleGattCallback = rxBleGattCallback;
            this.bluetoothGattProvider = bluetoothGattProvider;
            this.connectTimeout = connectionTimeout;
        }

        public Builder setAutoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
            return this;
        }

        public RxBleRadioOperationConnect build() {
            return new RxBleRadioOperationConnect(bluetoothDevice, connectionCompat, rxBleGattCallback, bluetoothGattProvider,
                    connectTimeout, autoConnect);
        }
    }

    private final BluetoothDevice bluetoothDevice;
    private final BleConnectionCompat connectionCompat;
    private final RxBleGattCallback rxBleGattCallback;
    private final BluetoothGattProvider bluetoothGattProvider;
    private final TimeoutConfiguration connectTimeout;
    private final boolean autoConnect;
    private final Runnable releaseRadioRunnable = new Runnable() {
        @Override
        public void run() {
            releaseRadio();
        }
    };
    private final Runnable emptyRunnable = new Runnable() {
        @Override
        public void run() {
        }
    };

    private final BehaviorSubject<Boolean> isSubscribed = BehaviorSubject.create();

    private final Observable<BluetoothGatt> operationConnectAsObservableWithSubscribersMonitoring = super.asObservable()
            .doOnSubscribe(new Action0() {
                @Override
                public void call() {
                    isSubscribed.onNext(true);
                }
            })
            .doOnUnsubscribe(new Action0() {
                @Override
                public void call() {
                    isSubscribed.onNext(false);
                }
            })
            .share();

    @Inject
    RxBleRadioOperationConnect(
            BluetoothDevice bluetoothDevice,
            BleConnectionCompat connectionCompat,
            RxBleGattCallback rxBleGattCallback,
            BluetoothGattProvider bluetoothGattProvider,
            @Named(CONNECT_TIMEOUT) TimeoutConfiguration connectTimeout,
            boolean autoConnect) {
        this.bluetoothDevice = bluetoothDevice;
        this.connectionCompat = connectionCompat;
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGattProvider = bluetoothGattProvider;
        this.connectTimeout = connectTimeout;
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
                .compose(wrapWithTimeoutWhenNotAutoconnecting())
                // when there are no subscribers there is no point of continuing work -> next will be disconnect operation
                .takeUntil(asObservableHasNoSubscribers().doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean noSubscribers) {
                        RxBleLog.d("No subscribers, finishing operation");
                    }
                }))
                .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        onConnectionEstablishedRunnable.run();
                    }
                })
                .doOnNext(new Action1<BluetoothGatt>() {
                    @Override
                    public void call(BluetoothGatt ignored) {
                        isSubscribed.onCompleted();
                    }
                })
                .subscribe(getSubscriber());
        onConnectCalledRunnable.run();
    }

    private Observable.Transformer<BluetoothGatt, BluetoothGatt> wrapWithTimeoutWhenNotAutoconnecting() {
        return new Observable.Transformer<BluetoothGatt, BluetoothGatt>() {
            @Override
            public Observable<BluetoothGatt> call(Observable<BluetoothGatt> bluetoothGattObservable) {
                return autoConnect
                        ? bluetoothGattObservable
                        : bluetoothGattObservable
                        .timeout(connectTimeout.timeout, connectTimeout.timeoutTimeUnit,
                                prepareConnectionTimeoutErrorObservable(), connectTimeout.timeoutScheduler);
            }
        };
    }

    @NonNull
    private Observable<BluetoothGatt> prepareConnectionTimeoutErrorObservable() {
        return Observable.error(
                new BleGattCallbackTimeoutException(bluetoothGattProvider.getBluetoothGatt(), BleGattOperationType.CONNECTION_STATE));
    }

    @NonNull
    private Observable<Boolean> asObservableHasNoSubscribers() {
        return isSubscribed.filter(new Func1<Boolean, Boolean>() {
            @Override
            public Boolean call(Boolean aBoolean) {
                return !aBoolean;
            }
        });
    }

    /**
     * Emits BluetoothGatt and completes after connection is established.
     *
     * @return BluetoothGatt after connection reaches {@link com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState#CONNECTED}
     * state.
     * @throws com.polidea.rxandroidble.exceptions.BleDisconnectedException if connection was disconnected/failed before it was established.
     */
    @NonNull
    private Observable<BluetoothGatt> getConnectedBluetoothGatt() {
        // start connecting the BluetoothGatt
        // note: Due to different Android BLE stack implementations it is not certain whether `connectGatt()` or `BluetoothGattCallback`
        // will emit BluetoothGatt first
        return connectGatt()
                // disconnect may happen even if the connection was not established yet
                .mergeWith(rxBleGattCallback.<BluetoothGatt>observeDisconnect())
                // capture BluetoothGatt when connected
                .sample(rxBleGattCallback
                        .getOnConnectionStateChange()
                        .filter(new Func1<RxBleConnection.RxBleConnectionState, Boolean>() {
                            @Override
                            public Boolean call(RxBleConnection.RxBleConnectionState rxBleConnectionState) {
                                return rxBleConnectionState == CONNECTED;
                            }
                        }))
                .take(1);
    }

    @NonNull
    private Observable<BluetoothGatt> connectGatt() {
        return Observable.fromCallable(
                new Callable<BluetoothGatt>() {
                    @Override
                    public BluetoothGatt call() throws Exception {
                        final BluetoothGatt bluetoothGatt = connectionCompat
                                .connectGatt(bluetoothDevice, autoConnect, rxBleGattCallback.getBluetoothGattCallback());
                        // Capture BluetoothGatt when connection is initiated.
                        bluetoothGattProvider.updateBluetoothGatt(bluetoothGatt);
                        return bluetoothGatt;
                    }
                }
        );
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleDisconnectedException(deadObjectException, bluetoothDevice.getAddress());
    }
}
