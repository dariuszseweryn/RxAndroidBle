package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.BleConnectionCompat;

import java.util.concurrent.Callable;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTED;

public class RxBleRadioOperationConnect extends RxBleRadioOperation<BluetoothGatt> {

    private final BluetoothDevice bluetoothDevice;

    private final RxBleGattCallback rxBleGattCallback;

    private final BleConnectionCompat connectionCompat;

    private final boolean autoConnect;

    private BehaviorSubject<BluetoothGatt> bluetoothGattBehaviorSubject = BehaviorSubject.create();

    private final Runnable releaseRadioRunnable = new Runnable() {
        @Override
        public void run() {
            RxBleRadioOperationConnect.this.releaseRadio();
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
     * @return BluetoothGatt after connection reaches {@link com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState#CONNECTED} state.
     * @throws com.polidea.rxandroidble.exceptions.BleDisconnectedException if connection was disconnected/failed before it was established.
     */
    @NonNull
    private Observable<BluetoothGatt> getConnectedBluetoothGatt() {
        // start connecting the BluetoothGatt
        // note: Due to different Android BLE stack implementations it is not certain whether `connectGatt()` or `BluetoothGattCallback`
        // will emit BluetoothGatt first
        return Observable.fromCallable(
                new Callable<BluetoothGatt>() {
                    @Override
                    public BluetoothGatt call() throws Exception {
                        return connectionCompat.connectGatt(bluetoothDevice, autoConnect, rxBleGattCallback.getBluetoothGattCallback());
                    }
                }
        )
                .mergeWith(rxBleGattCallback.getBluetoothGatt())
                // relay BluetoothGatt instance updates
                .doOnNext(new Action1<BluetoothGatt>() {
                    @Override
                    public void call(BluetoothGatt gatt) {
                        bluetoothGattBehaviorSubject.onNext(gatt);
                    }
                })
                // finish relaying if there won't be more updates
                .doOnTerminate(new Action0() {
                    @Override
                    public void call() {
                        bluetoothGattBehaviorSubject.onCompleted();
                    }
                })
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
                .take(1)
                // finish relaying if there won't be more updates
                .doOnTerminate(new Action0() {
                    @Override
                    public void call() {
                        bluetoothGattBehaviorSubject.onCompleted();
                    }
                });
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
