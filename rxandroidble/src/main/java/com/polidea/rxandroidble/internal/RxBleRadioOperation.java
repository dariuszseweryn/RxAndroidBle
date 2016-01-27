package com.polidea.rxandroidble.internal;

import java.util.concurrent.Semaphore;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;

public abstract class RxBleRadioOperation<T> implements Runnable {

    private PublishSubject<T> publishSubject = PublishSubject.create();

    private Semaphore radioBlockingSemaphore;

    public final Observable<T> asObservable() {
        return publishSubject;
    }

    protected final Subscriber<T> getSubscriber() {
        return new Subscriber<T>() {
            @Override
            public void onCompleted() {
                RxBleRadioOperation.this.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                RxBleRadioOperation.this.onError(e);
            }

            @Override
            public void onNext(T t) {
                RxBleRadioOperation.this.onNext(t);
            }
        };
    }

    protected final void onNext(T next) {
        publishSubject.onNext(next);
    }

    protected final void onCompleted() {
        publishSubject.onCompleted();
    }

    protected final void onError(Throwable throwable) {
        publishSubject.onError(throwable);
    }

    public void setRadioBlockingSemaphore(Semaphore radioBlockingSemaphore) {
        this.radioBlockingSemaphore = radioBlockingSemaphore;
    }

    protected final void releaseRadio() {
        radioBlockingSemaphore.release();
    }
}
