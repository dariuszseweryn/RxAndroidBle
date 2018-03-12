package com.polidea.rxandroidble2.internal.util;

import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;
import io.reactivex.SingleEmitter;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;

public class DisposableUtil {

    private DisposableUtil() {
    }

    public static <T> DisposableSingleObserver<T> disposableSingleObserverFromEmitter(final SingleEmitter<T> emitter) {
        return new DisposableSingleObserver<T>() {

            @Override
            public void onSuccess(T t) {
                if (!emitter.isDisposed()) {
                    emitter.onSuccess(t);
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(e);
                }
            }
        };
    }

    public static <T> DisposableObserver<T> disposableObserverFromEmitter(final ObservableEmitter<T> emitter) {
        return new DisposableObserver<T>() {

            @Override
            public void onNext(T t) {
                if (!emitter.isDisposed()) {
                    emitter.onNext(t);
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(e);
                }
            }

            @Override
            public void onComplete() {
                if (!emitter.isDisposed()) {
                    emitter.onComplete();
                }
            }
        };
    }

    public static <T> DisposableSingleObserver<T> disposableSingleObserverFromEmitter(final ObservableEmitter<T> emitter) {
        return new DisposableSingleObserver<T>() {

            @Override
            public void onSuccess(T t) {
                if (!emitter.isDisposed()) {
                    emitter.onNext(t);
                    emitter.onComplete();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(e);
                }
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
