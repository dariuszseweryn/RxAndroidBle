package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Actions;
import rx.functions.Func0;

public class RxBleConnectionConnectorImpl implements RxBleConnection.Connector {

    private final RxBleRadio rxBleRadio;
    private final ConnectionComponent.Builder connectionComponentBuilder;

    @Inject
    public RxBleConnectionConnectorImpl(
            RxBleRadio rxBleRadio,
            ConnectionComponent.Builder connectionComponentBuilder) {
        this.rxBleRadio = rxBleRadio;
        this.connectionComponentBuilder = connectionComponentBuilder;
    }

    @Override
    public Observable<RxBleConnection> prepareConnection(final boolean autoConnect) {
        return Observable.defer(new Func0<Observable<RxBleConnection>>() {
            @Override
            public Observable<RxBleConnection> call() {

                final ConnectionComponent connectionComponent = connectionComponentBuilder.build();
                RxBleRadioOperationConnect operationConnect = connectionComponent.connectOperationBuilder()
                        .setAutoConnect(autoConnect)
                        .build();

                final RxBleConnection connection = connectionComponent.rxBleConnection();
                final Observable<BluetoothGatt> connectedObservable = rxBleRadio.queue(operationConnect);
                final Observable<RxBleConnection> disconnectedErrorObservable = connectionComponent.gattCallback().observeDisconnect();
                final Action0 disconnect = queueIgnoringResult(connectionComponent.disconnectOperation());

                return Observable.just(connection)
                        .delaySubscription(connectedObservable)
                        .mergeWith(disconnectedErrorObservable)
                        .doOnUnsubscribe(disconnect);
            }

            @NonNull
            private Action0 queueIgnoringResult(final RxBleRadioOperationDisconnect operationDisconnect) {
                return new Action0() {
                    @Override
                    public void call() {
                        rxBleRadio
                                .queue(operationDisconnect)
                                .subscribe(
                                        Actions.empty(),
                                        Actions.<Throwable>toAction1(Actions.empty())
                                );
                    }
                };
            }
        });
    }
}
