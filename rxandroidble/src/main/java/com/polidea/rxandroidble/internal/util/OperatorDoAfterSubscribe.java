package com.polidea.rxandroidble.internal.util;


import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.observers.Subscribers;
import rx.schedulers.Schedulers;

/**
 * This operator modifies an {@link rx.Observable} so a given action is invoked after the {@link rx.Observable} is subscribed.
 * The thread which is used to execute the action is captured at the construction time.
 * @param <T> The type of the elements in the {@link rx.Observable} that this operator modifies.
 */
public class OperatorDoAfterSubscribe<T> implements Observable.Operator<T, T> {

    private final Action0 subscribe;

    private final Scheduler.Worker worker = Schedulers.trampoline().createWorker();

    /**
     * Constructs an instance of the operator with the callback that gets invoked when the modified Observable is subscribed
     * @param subscribe the action that gets invoked when the modified {@link rx.Observable} is subscribed
     */
    public OperatorDoAfterSubscribe(Action0 subscribe) {
        this.subscribe = subscribe;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        worker.schedule(subscribe);
        // Pass through since this operator is for notification only, there is
        // no change to the stream whatsoever.
        return Subscribers.wrap(child);
    }
}
