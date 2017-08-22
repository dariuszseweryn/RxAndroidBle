package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleAdapterStateObservable.BleAdapterState;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.internal.ConnectionSetup;
import com.polidea.rxandroidble.internal.operations.ConnectOperation;
import com.polidea.rxandroidble.internal.serialization.ClientOperationQueue;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import static com.polidea.rxandroidble.internal.util.ObservableUtil.justOnNext;

public class ConnectorImpl implements Connector {

    private final BluetoothDevice bluetoothDevice;
    private final ClientOperationQueue clientOperationQueue;
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final Observable<BleAdapterState> adapterStateObservable;
    private final ConnectionComponent.Builder connectionComponentBuilder;

    @Inject
    public ConnectorImpl(
            BluetoothDevice bluetoothDevice,
            ClientOperationQueue clientOperationQueue,
            RxBleAdapterWrapper rxBleAdapterWrapper,
            Observable<BleAdapterState> adapterStateObservable,
            ConnectionComponent.Builder connectionComponentBuilder) {
        this.bluetoothDevice = bluetoothDevice;
        this.clientOperationQueue = clientOperationQueue;
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.adapterStateObservable = adapterStateObservable;
        this.connectionComponentBuilder = connectionComponentBuilder;
    }

    @Override
    public Observable<RxBleConnection> prepareConnection(final ConnectionSetup options) {
        return Observable.defer(new Func0<Observable<RxBleConnection>>() {
            @Override
            public Observable<RxBleConnection> call() {

                if (!rxBleAdapterWrapper.isBluetoothEnabled()) {
                    return Observable.error(new BleDisconnectedException(bluetoothDevice.getAddress()));
                }

                final ConnectionComponent connectionComponent = connectionComponentBuilder
                        .connectionModule(new ConnectionModule(options))
                        .build();
                ConnectOperation operationConnect = connectionComponent.connectOperation();

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
                        .doOnUnsubscribe(connectionComponent.disconnectAction());
            }

            @NonNull
            private Observable<BluetoothGatt> enqueueConnectOperation(ConnectOperation operationConnect) {
                return Observable
                        .merge(
                                clientOperationQueue.queue(operationConnect),
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
}
