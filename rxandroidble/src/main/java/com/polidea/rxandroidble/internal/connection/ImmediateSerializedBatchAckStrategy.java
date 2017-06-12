package com.polidea.rxandroidble.internal.connection;

import com.polidea.rxandroidble.RxBleConnection;

import rx.Observable;

public class ImmediateSerializedBatchAckStrategy implements RxBleConnection.WriteOperationAckStrategy {

    @Override
    public Observable<Boolean> call(Observable<Boolean> objectObservable) {
        return objectObservable;
    }
}
