package com.polidea.rxandroidble.internal.connection;


import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.functions.Action1;
import rx.subscriptions.SerialSubscription;

@ConnectionScope
class MtuWatcher implements ConnectionSubscriptionWatcher, MtuProvider, Action1<Integer> {

    private Integer currentMtu;
    private final Observable<Integer> mtuObservable;
    private final SerialSubscription serialSubscription = new SerialSubscription();

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
        serialSubscription.unsubscribe();
    }

    @Override
    public void call(Integer newMtu) {
        this.currentMtu = newMtu;
    }
}
