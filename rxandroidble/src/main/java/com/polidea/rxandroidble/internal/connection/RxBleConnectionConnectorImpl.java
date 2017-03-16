package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Actions;
import rx.functions.Func0;
import rx.functions.Func1;

import static com.polidea.rxandroidble.internal.util.ObservableUtil.justOnNext;

public class RxBleConnectionConnectorImpl implements RxBleConnection.Connector {

    private final BluetoothDevice bluetoothDevice;
    private final RxBleRadio rxBleRadio;
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final Observable<BleAdapterState> adapterStateObservable;
    private final ConnectionComponent.Builder connectionComponentBuilder;

    @Inject
    public RxBleConnectionConnectorImpl(
            BluetoothDevice bluetoothDevice,
            RxBleRadio rxBleRadio,
            RxBleAdapterWrapper rxBleAdapterWrapper,
            Observable<BleAdapterState> adapterStateObservable,
            ConnectionComponent.Builder connectionComponentBuilder) {
        this.bluetoothDevice = bluetoothDevice;
        this.rxBleRadio = rxBleRadio;
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.adapterStateObservable = adapterStateObservable;
        this.connectionComponentBuilder = connectionComponentBuilder;
    }

    @Override
    public Observable<RxBleConnection> prepareConnection(final boolean autoConnect) {
        return Observable.defer(new Func0<Observable<RxBleConnection>>() {
            @Override
            public Observable<RxBleConnection> call() {

                if (!rxBleAdapterWrapper.isBluetoothEnabled()) {
                    return Observable.error(new BleDisconnectedException(bluetoothDevice.getAddress()));
                }

                final ConnectionComponent connectionComponent = connectionComponentBuilder.build();
                RxBleRadioOperationConnect operationConnect = connectionComponent.connectOperationBuilder()
                        .setAutoConnect(autoConnect)
                        .build();

                return enqueueConnectOperation(operationConnect)
                        .flatMap(new Func1<BluetoothGatt, Observable<RxBleConnection>>() {
                            @Override
                            public Observable<RxBleConnection> call(final BluetoothGatt bluetoothGatt) {
                                return Observable.merge(
                                        justOnNext(connectionComponent.rxBleConnection()),
                                        connectionComponent.gattCallback().<RxBleConnection>observeDisconnect()
                                );
                            }
                        })
                        .doOnUnsubscribe(disconnect(connectionComponent.disconnectOperation()));
            }

            @NonNull
            private Action0 disconnect(final RxBleRadioOperationDisconnect operationDisconnect) {
                return new Action0() {
                    @Override
                    public void call() {
                        enqueueDisconnectOperation(operationDisconnect);
                    }
                };
            }

            @NonNull
            private Observable<BluetoothGatt> enqueueConnectOperation(RxBleRadioOperationConnect operationConnect) {
                return Observable
                        .merge(
                                rxBleRadio.queue(operationConnect),
                                adapterNotUsableObservable()
                                        .flatMap(new Func1<BleAdapterState, Observable<BluetoothGatt>>() {
                                            @Override
                                            public Observable<BluetoothGatt> call(BleAdapterState bleAdapterState) {
                                                return Observable.error(new BleDisconnectedException(bluetoothDevice.getAddress()));
                                            }
                                        })
                        )
                        .first();
            }
        });
    }

    private Observable<BleAdapterState> adapterNotUsableObservable() {
        return adapterStateObservable
                .filter(new Func1<BleAdapterState, Boolean>() {
                    @Override
                    public Boolean call(BleAdapterState bleAdapterState) {
                        return !bleAdapterState.isUsable();
                    }
                });
    }

    private Subscription enqueueDisconnectOperation(RxBleRadioOperationDisconnect operationDisconnect) {
        return rxBleRadio
                .queue(operationDisconnect)
                .subscribe(
                        Actions.empty(),
                        Actions.<Throwable>toAction1(Actions.empty())
                );
    }
}
