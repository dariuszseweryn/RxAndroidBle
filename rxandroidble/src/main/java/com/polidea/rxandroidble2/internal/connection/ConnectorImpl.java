package com.polidea.rxandroidble2.internal.connection;

import android.bluetooth.BluetoothGatt;

import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.ConnectionSetup;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.internal.serialization.ClientOperationQueue;

import java.util.Set;
import java.util.concurrent.Callable;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class ConnectorImpl implements Connector {

    private final ClientOperationQueue clientOperationQueue;
    private final ConnectionComponent.Builder connectionComponentBuilder;
    private final Scheduler callbacksScheduler;

    @Inject
    public ConnectorImpl(
            ClientOperationQueue clientOperationQueue,
            ConnectionComponent.Builder connectionComponentBuilder,
            @Named(ClientComponent.NamedSchedulers.BLUETOOTH_CALLBACKS) Scheduler callbacksScheduler) {
        this.clientOperationQueue = clientOperationQueue;
        this.connectionComponentBuilder = connectionComponentBuilder;
        this.callbacksScheduler = callbacksScheduler;
    }

    @Override
    public Observable<RxBleConnection> prepareConnection(final ConnectionSetup options) {
        return Observable.defer(new Callable<ObservableSource<RxBleConnection>>() {
            @Override
            public ObservableSource<RxBleConnection> call() throws Exception {
                final ConnectionComponent connectionComponent = connectionComponentBuilder
                        .connectionModule(new ConnectionModule(options))
                        .build();

                final Set<ConnectionSubscriptionWatcher> connSubWatchers = connectionComponent.connectionSubscriptionWatchers();
                return enqueueConnectOperation(connectionComponent)
                        .flatMap(new Function<BluetoothGatt, ObservableSource<RxBleConnection>>() {
                            @Override
                            public ObservableSource<RxBleConnection> apply(BluetoothGatt bluetoothGatt) throws Exception {
                                return Observable.merge(
                                        obtainRxBleConnection(connectionComponent),
                                        observeDisconnections(connectionComponent)
                                );
                            }
                        })
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) throws Exception {
                                for (ConnectionSubscriptionWatcher csa : connSubWatchers) {
                                    csa.onConnectionSubscribed();
                                }
                            }
                        })
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                for (ConnectionSubscriptionWatcher csa : connSubWatchers) {
                                    csa.onConnectionUnsubscribed();
                                }
                            }
                        })
                        .subscribeOn(callbacksScheduler)
                        .unsubscribeOn(callbacksScheduler);
            }
        });
    }

    private static Observable<RxBleConnection> obtainRxBleConnection(final ConnectionComponent connectionComponent) {
        return Observable.fromCallable(new Callable<RxBleConnection>() {
            @Override
            public RxBleConnection call() throws Exception {
                // BluetoothGatt is needed for RxBleConnection
                // BluetoothGatt is produced by RxBleRadioOperationConnect
                return connectionComponent.rxBleConnection();
            }
        });
    }

    private static Observable<RxBleConnection> observeDisconnections(ConnectionComponent connectionComponent) {
        return connectionComponent.gattCallback().observeDisconnect();
    }

    private Observable<BluetoothGatt> enqueueConnectOperation(ConnectionComponent connectionComponent) {
        return clientOperationQueue.queue(connectionComponent.connectOperation());
    }
}
