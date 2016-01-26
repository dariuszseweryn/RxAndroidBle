package com.polidea.rxandroidble.internal;

import java.util.concurrent.atomic.AtomicReference;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;

public class RxBleRadioImpl implements RxBleRadio {

    private BehaviorSubject<Boolean> previouslyScheduledObservableFinished = BehaviorSubject.create(true);

    @Override
    public <T> Observable<T> scheduleRadioObservable(Observable<T> radioBlockingObservable) {
        AtomicReference<Subscription> subscriptionAtomicReference = new AtomicReference<>();
        final Observable<T> tObservable = Observable.create(subscriber -> {
            final BehaviorSubject<Boolean> newPreviouslyObservableFinishedSubject = BehaviorSubject.create();
            final Subscription subscription = previouslyScheduledObservableFinished
                    .flatMap(aBoolean -> radioBlockingObservable)
                    .doOnTerminate(() -> newPreviouslyObservableFinishedSubject.onNext(true))
                    .doOnUnsubscribe(() -> newPreviouslyObservableFinishedSubject.onNext(true))
                    .subscribe(subscriber);
            subscriptionAtomicReference.set(subscription);
            previouslyScheduledObservableFinished = newPreviouslyObservableFinishedSubject;
        });
        return tObservable.doOnUnsubscribe(() -> subscriptionAtomicReference.get().unsubscribe());
    }
}
