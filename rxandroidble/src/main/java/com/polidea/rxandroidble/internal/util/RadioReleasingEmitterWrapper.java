package com.polidea.rxandroidble.internal.util;


import com.polidea.rxandroidble.internal.RadioReleaseInterface;
import java.util.concurrent.atomic.AtomicBoolean;
import rx.Emitter;
import rx.Observer;
import rx.functions.Cancellable;

/**
 * A convenience class to use in {@link com.polidea.rxandroidble.internal.RxBleRadioOperation} subclasses. It wraps the {@link Emitter}
 * and {@link RadioReleaseInterface} and makes sure that the {@link rx.Subscription} it was subscribed to will finish and call
 * {@link RadioReleaseInterface#release()} in either {@link #onCompleted()} or {@link #onError(Throwable)} in case of the wrapped emitter
 * being unsubscribed / canceled.
 * @param <T> parameter of the wrapped {@link Emitter}
 */
public class RadioReleasingEmitterWrapper<T> implements Observer<T>, Cancellable {

    private final AtomicBoolean isEmitterCanceled = new AtomicBoolean(false);

    private final Emitter<T> emitter;

    private final RadioReleaseInterface radioReleaseInterface;

    public RadioReleasingEmitterWrapper(Emitter<T> emitter, RadioReleaseInterface radioReleaseInterface) {
        this.emitter = emitter;
        this.radioReleaseInterface = radioReleaseInterface;
        emitter.setCancellation(this);
    }

    @Override
    public void onCompleted() {
        radioReleaseInterface.release();
        emitter.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        radioReleaseInterface.release();
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
