package com.polidea.rxandroidble.internal.connection;

import com.polidea.rxandroidble.RxBleConnection;

import rx.Observable;

public class ImmediateSerializedBatchActStrategy implements RxBleConnection.WriteOperationAckStrategy {

    @Override
    public Observable<Boolean> call(Observable<Boolean> objectObservable) {
        return objectObservable;
    }
}
