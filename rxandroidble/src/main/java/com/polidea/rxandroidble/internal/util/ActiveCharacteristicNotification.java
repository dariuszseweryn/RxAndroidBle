package com.polidea.rxandroidble.internal.util;


import rx.Observable;

public class ActiveCharacteristicNotification {

    public final Observable<Observable<byte[]>> notificationObservable;

    public final boolean isIndication;

    public ActiveCharacteristicNotification(Observable<Observable<byte[]>> notificationObservable, boolean isIndication) {
        this.notificationObservable = notificationObservable;
        this.isIndication = isIndication;
    }
}
