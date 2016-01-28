package com.polidea.rxandroidble.internal;

import android.support.annotation.NonNull;
import java.util.concurrent.Semaphore;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.PublishSubject;

public abstract class RxBleRadioOperation<T> implements Runnable, Comparable<RxBleRadioOperation> {

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

    protected Priority definedPriority() {
        return Priority.NORMAL;
    }

    @Override
    public int compareTo(@NonNull RxBleRadioOperation another) {
        return another.definedPriority().priority - definedPriority().priority;
    }

    public static class Priority {

        public static Priority HIGH = new Priority(100);

        public static Priority NORMAL = new Priority(50);

        public static Priority LOW = new Priority(0);

        private final int priority;

        private Priority(int priority) {

            this.priority = priority;
        }
    }
}
