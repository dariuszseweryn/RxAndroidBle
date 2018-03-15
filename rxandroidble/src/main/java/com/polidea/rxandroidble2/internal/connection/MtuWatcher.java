package com.polidea.rxandroidble2.internal.connection;


import com.polidea.rxandroidble2.exceptions.BleGattException;
import com.polidea.rxandroidble2.exceptions.BleGattOperationType;
import com.polidea.rxandroidble2.internal.RxBleLog;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;

@ConnectionScope
class MtuWatcher implements ConnectionSubscriptionWatcher, MtuProvider, Consumer<Integer> {

    private Integer currentMtu;
    private final Observable<Integer> mtuObservable;

    @Inject
    MtuWatcher(
            final RxBleGattCallback rxBleGattCallback,
            @Named(ConnectionComponent.NamedInts.GATT_MTU_MINIMUM) final int initialValue
    ) {
        this.mtuObservable = rxBleGattCallback.getOnMtuChanged()
                .retry(new Predicate<Throwable>() {
                    @Override
                    public boolean test(Throwable throwable) throws Exception {
                        return throwable instanceof BleGattException
                                && ((BleGattException) throwable).getBleGattOperationType() == BleGattOperationType.ON_MTU_CHANGED;
                    }
                });
        this.currentMtu = initialValue;
    }

    @Override
    public int getMtu() {
        return currentMtu;
    }

    @Override
    public void onConnectionSubscribed() {
        mtuObservable.subscribe(this, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                RxBleLog.d(throwable, "An error received when listening to MTU changes.");
            }
        });
    }

    @Override
    public void onConnectionUnsubscribed() {
        // Not required
    }

    @Override
    public void accept(Integer newMtu) {
        this.currentMtu = newMtu;
    }
}
