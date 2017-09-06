package com.polidea.rxandroidble.internal.connection;

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

import static com.polidea.rxandroidble.internal.util.ObservableUtil.justOnNext;

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

                return justOnNext(connectionComponent.rxBleConnection())
                        .delaySubscription(rxBleRadio.queue(operationConnect))
                        .doOnUnsubscribe(disconnect(connectionComponent.disconnectOperation()))
                        .mergeWith(connectionComponent.gattCallback().<RxBleConnection>observeDisconnect());
            }

            @NonNull
            private Action0 disconnect(final RxBleRadioOperationDisconnect operationDisconnect) {
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
