package com.polidea.rxandroidble3.internal.util;

import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.observers.DisposableSingleObserver;

public class DisposableUtil {

    private DisposableUtil() {
    }

    public static <T> DisposableSingleObserver<T> disposableSingleObserverFromEmitter(final SingleEmitter<T> emitter) {
        return new DisposableSingleObserver<T>() {

            @Override
            public void onSuccess(T t) {
                emitter.onSuccess(t);
            }

            @Override
            public void onError(Throwable e) {
                emitter.tryOnError(e);
            }
        };
    }

    public static <T> DisposableObserver<T> disposableObserverFromEmitter(final ObservableEmitter<T> emitter) {
        return new DisposableObserver<T>() {

            @Override
            public void onNext(T t) {
                emitter.onNext(t);
            }

            @Override
            public void onError(Throwable e) {
                emitter.tryOnError(e);
            }

            @Override
            public void onComplete() {
                emitter.onComplete();
            }
        };
    }

    public static <T> DisposableSingleObserver<T> disposableSingleObserverFromEmitter(final ObservableEmitter<T> emitter) {
        return new DisposableSingleObserver<T>() {

            @Override
            public void onSuccess(T t) {
                emitter.onNext(t);
                emitter.onComplete();
            }

            @Override
            public void onError(Throwable e) {
                emitter.tryOnError(e);
            }
        };
    }
}
