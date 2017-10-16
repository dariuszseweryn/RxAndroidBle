package com.polidea.rxandroidble.internal.connection;


import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Completable;
import rx.CompletableSubscriber;
import rx.Subscription;
import rx.functions.Action1;

@ConnectionScope
class MtuWatcher extends Completable implements MtuProvider {

    private final AtomicInteger currentMtuAtomicInteger;

    @Inject
    MtuWatcher(
            final RxBleGattCallback rxBleGattCallback,
            final AtomicInteger atomicInteger,
            @Named(ConnectionComponent.NamedInts.GATT_WRITE_MTU_OVERHEAD) final int initialValue
    ) {
        super(new OnSubscribe() {
            @Override
            public void call(CompletableSubscriber completableSubscriber) {
                Subscription mtuSubscription = rxBleGattCallback.getOnMtuChanged()
                        .retry()
                        .subscribe(new Action1<Integer>() {
                            @Override
                            public void call(Integer newMtu) {
                                atomicInteger.set(newMtu);
                            }
                        });
                completableSubscriber.onSubscribe(mtuSubscription);
            }
        });
        atomicInteger.set(initialValue);
        this.currentMtuAtomicInteger = atomicInteger;
    }

    @Override
    public int getMtu() {
        return currentMtuAtomicInteger.get();
    }
}
