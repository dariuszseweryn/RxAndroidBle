package com.polidea.rxandroidble2.internal.connection;

import com.polidea.rxandroidble2.internal.operations.DisconnectOperation;
import com.polidea.rxandroidble2.internal.serialization.ClientOperationQueue;

import bleshadow.javax.inject.Inject;

import io.reactivex.functions.Action;

@ConnectionScope
class DisconnectAction implements ConnectionSubscriptionWatcher {

    private final ClientOperationQueue clientOperationQueue;
    private final DisconnectOperation operationDisconnect;
    private final DisconnectionRouterInput disconnectionRouterInput;

    @Inject
    DisconnectAction(
            ClientOperationQueue clientOperationQueue,
            DisconnectOperation operationDisconnect,
            DisconnectionRouterInput disconnectionRouterInput) {
        this.clientOperationQueue = clientOperationQueue;
        this.operationDisconnect = operationDisconnect;
        this.disconnectionRouterInput = disconnectionRouterInput;
    }

    @Override
    public void onConnectionSubscribed() {
        // do nothing
    }

    @Override
    public void onConnectionUnsubscribed() {
        clientOperationQueue
                .queue(operationDisconnect)
                .ignoreElements()
                .onErrorComplete()
                .subscribe(new Action() {
                    @Override
                    public void run() {
                        disconnectionRouterInput.close();
                    }
                });
    }
}
