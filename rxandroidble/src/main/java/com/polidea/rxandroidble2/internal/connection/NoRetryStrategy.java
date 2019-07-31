package com.polidea.rxandroidble2.internal.connection;

import com.polidea.rxandroidble2.RxBleConnection;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Function;

public class NoRetryStrategy implements RxBleConnection.WriteOperationRetryStrategy {

    @Override
    public Observable<LongWriteFailure> apply(Observable<LongWriteFailure> observable) {
        return observable.flatMap(new Function<LongWriteFailure, Observable<LongWriteFailure>>() {
            @Override
            public Observable<LongWriteFailure> apply(LongWriteFailure longWriteFailure) {
                return Observable.error(longWriteFailure.getCause());
            }
        });
    }
}
