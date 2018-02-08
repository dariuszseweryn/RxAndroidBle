package com.polidea.rxandroidble.internal.connection;


import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

import io.reactivex.Observable;
import io.reactivex.disposables.SerialDisposable;
import io.reactivex.functions.Consumer;

@ConnectionScope
class MtuWatcher implements ConnectionSubscriptionWatcher, MtuProvider, Consumer<Integer> {

    private Integer currentMtu;
    private final Observable<Integer> mtuObservable;
    private final SerialDisposable serialSubscription = new SerialDisposable();

    @Inject
    MtuWatcher(
            final RxBleGattCallback rxBleGattCallback,
            @Named(ConnectionComponent.NamedInts.GATT_MTU_MINIMUM) final int initialValue
    ) {
        this.mtuObservable = rxBleGattCallback.getOnMtuChanged().retry();
        this.currentMtu = initialValue;
    }

    @Override
    public int getMtu() {
        return currentMtu;
    }

    @Override
    public void onConnectionSubscribed() {
        serialSubscription.set(mtuObservable.subscribe(this));
    }

    @Override
    public void onConnectionUnsubscribed() {
        serialSubscription.dispose();
    }

    @Override
    public void accept(Integer newMtu) {
        this.currentMtu = newMtu;
    }
}
