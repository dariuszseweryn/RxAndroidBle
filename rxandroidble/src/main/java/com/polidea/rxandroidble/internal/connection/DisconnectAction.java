package com.polidea.rxandroidble.internal.connection;

import com.polidea.rxandroidble.internal.operations.DisconnectOperation;
import com.polidea.rxandroidble.internal.serialization.ClientOperationQueue;

import bleshadow.javax.inject.Inject;

import io.reactivex.internal.functions.Functions;

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
