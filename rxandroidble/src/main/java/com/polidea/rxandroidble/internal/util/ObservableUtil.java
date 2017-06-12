package com.polidea.rxandroidble.internal.util;

import rx.Observable;

public class ObservableUtil {

    private static final Observable.Transformer<?, ?> IDENTITY_TRANSFORMER
            = new Observable.Transformer<Object, Object>() {
        @Override
        public Observable<Object> call(Observable<Object> rxBleInternalScanResultObservable) {
            return rxBleInternalScanResultObservable;
        }
    };

    private ObservableUtil() {

    }

    public static <T> Observable<T> justOnNext(T onNext) {
        return Observable.<T>never().startWith(onNext);
    }

    public static <T> Observable.Transformer<T, T> identityTransformer() {
        //noinspection unchecked
        return (Observable.Transformer<T, T>) IDENTITY_TRANSFORMER;
    }
}
