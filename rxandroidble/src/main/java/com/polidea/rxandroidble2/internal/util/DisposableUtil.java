package com.polidea.rxandroidble2.internal.util;

import io.reactivex.Emitter;
import io.reactivex.Observer;
import io.reactivex.SingleEmitter;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;

public class DisposableUtil {

    private DisposableUtil() {
    }

    public static <T> DisposableSingleObserver<T> disposableSingleEmitter(final SingleEmitter<T> emitter) {
        return new DisposableSingleObserver<T>() {

            @Override
            public void onSuccess(T t) {
                emitter.onSuccess(t);
            }

            @Override
            public void onError(Throwable e) {
                emitter.onError(e);
            }
        };
    }

    public static <T> DisposableObserver<T> disposableEmitter(final Emitter<T> emitter) {
        return new DisposableObserver<T>() {

            @Override
            public void onNext(T t) {
                emitter.onNext(t);
            }

            @Override
            public void onError(Throwable e) {
                emitter.onError(e);
            }

            @Override
            public void onComplete() {
                emitter.onComplete();
            }
        };
    }

    public static <T> DisposableSingleObserver<T> disposableSingleEmitter(final Emitter<T> emitter) {
        return new DisposableSingleObserver<T>() {

            @Override
            public void onSuccess(T t) {
                emitter.onNext(t);
                emitter.onComplete();
            }

            @Override
            public void onError(Throwable e) {
                emitter.onError(e);
            }
        };
    }

    public static <T> DisposableObserver<T> disposableObserver(final Observer<T> emitter) {
        return new DisposableObserver<T>() {

            @Override
            public void onNext(T t) {
                emitter.onNext(t);
            }

            @Override
            public void onError(Throwable e) {
                emitter.onError(e);
            }

            @Override
            public void onComplete() {
                emitter.onComplete();
            }
        };
    }
}
