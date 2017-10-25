package com.polidea.rxandroidble.internal.util;


import com.polidea.rxandroidble.internal.QueueOperation;
import com.polidea.rxandroidble.internal.serialization.QueueReleaseInterface;
import java.util.concurrent.atomic.AtomicBoolean;
import rx.Emitter;
import rx.Observer;
import rx.functions.Cancellable;

/**
 * A convenience class to use in {@link QueueOperation} subclasses. It wraps the {@link Emitter}
 * and {@link QueueReleaseInterface} and makes sure that the {@link rx.Subscription} it was subscribed to will finish and call
 * {@link QueueReleaseInterface#release()} in either {@link #onCompleted()} or {@link #onError(Throwable)} in case of the wrapped emitter
 * being unsubscribed / canceled.
 * @param <T> parameter of the wrapped {@link Emitter}
 */
public class QueueReleasingEmitterWrapper<T> implements Observer<T>, Cancellable {

    private final AtomicBoolean isEmitterCanceled = new AtomicBoolean(false);

    private final Emitter<T> emitter;

    private final QueueReleaseInterface queueReleaseInterface;

    public QueueReleasingEmitterWrapper(Emitter<T> emitter, QueueReleaseInterface queueReleaseInterface) {
        this.emitter = emitter;
        this.queueReleaseInterface = queueReleaseInterface;
        emitter.setCancellation(this);
    }

    @Override
    public void onCompleted() {
        queueReleaseInterface.release();
        emitter.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        queueReleaseInterface.release();
        emitter.onError(e);
    }

    @Override
    public void onNext(T t) {
        emitter.onNext(t);
    }

    @Override
    synchronized public void cancel() throws Exception {
        isEmitterCanceled.set(true);
    }

    synchronized public boolean isWrappedEmitterUnsubscribed() {
        return isEmitterCanceled.get();
    }
}
