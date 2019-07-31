package com.polidea.rxandroidble2.internal.connection;

import com.polidea.rxandroidble2.internal.operations.DisconnectOperation;
import com.polidea.rxandroidble2.internal.serialization.ClientOperationQueue;

import bleshadow.javax.inject.Inject;

import io.reactivex.rxjava3.internal.functions.Functions;

@ConnectionScope
class DisconnectAction implements ConnectionSubscriptionWatcher {

    private final ClientOperationQueue clientOperationQueue;
    private final DisconnectOperation operationDisconnect;

    @Inject
    DisconnectAction(ClientOperationQueue clientOperationQueue, DisconnectOperation operationDisconnect) {
        this.clientOperationQueue = clientOperationQueue;
        this.operationDisconnect = operationDisconnect;
    }

    @Override
    public void onConnectionSubscribed() {
        // do nothing
    }

    @Override
    public void onConnectionUnsubscribed() {
        clientOperationQueue
                .queue(operationDisconnect)
                .subscribe(
                        Functions.emptyConsumer(),
                        Functions.emptyConsumer()
                );
    }
}
