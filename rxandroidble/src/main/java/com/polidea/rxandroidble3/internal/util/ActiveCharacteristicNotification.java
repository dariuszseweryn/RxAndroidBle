package com.polidea.rxandroidble2.internal.util;


import io.reactivex.rxjava3.core.Observable;

public class ActiveCharacteristicNotification {

    public final Observable<Observable<byte[]>> notificationObservable;

    public final boolean isIndication;

    public ActiveCharacteristicNotification(Observable<Observable<byte[]>> notificationObservable, boolean isIndication) {
        this.notificationObservable = notificationObservable;
        this.isIndication = isIndication;
    }
}
