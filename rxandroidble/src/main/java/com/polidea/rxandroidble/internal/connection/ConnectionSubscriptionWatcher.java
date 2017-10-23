package com.polidea.rxandroidble.internal.connection;


public interface ConnectionSubscriptionWatcher {

    void onConnectionSubscribed();

    void onConnectionUnsubscribed();
}
