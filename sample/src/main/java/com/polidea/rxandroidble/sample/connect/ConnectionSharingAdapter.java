package com.polidea.rxandroidble.sample.connect;

import com.polidea.rxandroidble.RxBleConnection;

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;

public class ConnectionSharingAdapter implements Observable.Transformer<RxBleConnection, RxBleConnection> {

    private final AtomicReference<Observable<RxBleConnection>> connectionObservable = new AtomicReference<>();

    @Override
    public Observable<RxBleConnection> call(Observable<RxBleConnection> source) {
        synchronized (connectionObservable) {
            final Observable<RxBleConnection> rxBleConnectionObservable = connectionObservable.get();

            if (rxBleConnectionObservable != null) {
                return rxBleConnectionObservable;
            }

            final Observable<RxBleConnection> newConnectionObservable = source
                    .doOnUnsubscribe(() -> connectionObservable.set(null))
                    .replay(1)
                    .refCount();
            connectionObservable.set(newConnectionObservable);
            return newConnectionObservable;
        }
    }
}
