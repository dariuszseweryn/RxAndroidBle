package com.polidea.rxandroidble.internal.connection;

import com.polidea.rxandroidble.RxBleConnection;

import io.reactivex.Observable;

public class ImmediateSerializedBatchAckStrategy implements RxBleConnection.WriteOperationAckStrategy {

    @Override
    public Observable<Boolean> apply(Observable<Boolean> objectObservable) {
        return objectObservable;
    }
}
