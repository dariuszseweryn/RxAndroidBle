package com.polidea.rxandroidble.internal.util;

import rx.Observable;

public class ObservableUtil {

    private ObservableUtil() {

    }

    public static <T> Observable<T> justOnNext(T onNext) {
        return Observable.<T>never().startWith(onNext);
    }
}
