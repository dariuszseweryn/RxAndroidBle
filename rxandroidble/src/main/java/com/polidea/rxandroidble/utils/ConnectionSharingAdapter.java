package com.polidea.rxandroidble.utils;

import com.polidea.rxandroidble.RxBleConnection;

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;

/**
 * Observable transformer that can be used to share connection between many subscribers.
 * <p>
 * Example use:
 * <pre>
 * UUID characteristicUUID = UUID.fromString("70a5bfcc-a0ec-4091-985e-d5506a31c921");
 * Observable<RxBleConnection> connectionObservable = bleDevice
 * .establishConnection(this, false)
 * .compose(new ConnectionSharingAdapter());
 *
 * connectionObservable
 * .flatMap(rxBleConnection -> rxBleConnection.setupNotification(characteristicUUID))
 * .flatMap(notificationObservable -> notificationObservable)
 * .subscribe(bytes -> {
 * // React on characteristic changes
 * });
 *
 * connectionObservable
 * .flatMap(rxBleConnection -> rxBleConnection.writeCharacteristic(characteristicUUID, "some text".getBytes()))
 * .subscribe();
 * </pre>
 *
 * @see com.polidea.rxandroidble.exceptions.BleAlreadyConnectedException
 */
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
