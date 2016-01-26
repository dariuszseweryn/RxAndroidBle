package com.polidea.rxandroidble;

import rx.Observable;
import rx.subjects.BehaviorSubject;

public class RxBleRadioImpl implements RxBleRadio {

    private BehaviorSubject<Boolean> previouslyScheduledObservableFinished = BehaviorSubject.create(true);

    @Override
    public <T> Observable<T> scheduleRadioObservable(Observable<T> radioBlockingObservable) {
        return Observable.create(subscriber -> {
            final BehaviorSubject<Boolean> newPreviouslyObservableFinishedSubject = BehaviorSubject.create();
            previouslyScheduledObservableFinished
                    .flatMap(aBoolean -> radioBlockingObservable)
                    .doOnTerminate(() -> newPreviouslyObservableFinishedSubject.onNext(true))
                    .doOnUnsubscribe(() -> newPreviouslyObservableFinishedSubject.onNext(true))
                    .subscribe(subscriber);
            previouslyScheduledObservableFinished = newPreviouslyObservableFinishedSubject;
        }); // TODO: pass onUnsubscribe
    }
}
