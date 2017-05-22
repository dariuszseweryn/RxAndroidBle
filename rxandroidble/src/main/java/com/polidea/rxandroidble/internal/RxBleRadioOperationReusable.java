package com.polidea.rxandroidble.internal;

import android.os.DeadObjectException;
import android.support.annotation.NonNull;

import com.jakewharton.rxrelay.PublishRelay;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.internal.operations.Operation;
import java.util.concurrent.Semaphore;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * The base class for all operations that are executed on the Bluetooth Radio.
 * This class is intended to be a kind of wrapper over an Observable (returned by function asObservable()).
 * Implements Runnable interface which will be called on the applications main thread.
 *
 * @param <T> What is returned from this operation onNext()
 */
public abstract class RxBleRadioOperationReusable<T> implements Operation<T> {

    private final PublishRelay<T> nextPublishSubject = PublishRelay.create();
    private final PublishRelay<Throwable> errorPublishSubject = PublishRelay.create();
    private final PublishRelay<Boolean> completePublishSubject = PublishRelay.create();
    private final Observable<T> errorObservable = errorPublishSubject.flatMap(
            new Func1<Throwable, Observable<T>>() {
                @Override
                public Observable<T> call(Throwable throwable) {
                    return Observable.error(throwable);
                }
            });

    private Semaphore radioBlockingSemaphore;

    /**
     * A function that returns this operation as an Observable.
     * When the returned observable will be subscribed this operation will be scheduled
     * to be run on the main thread in future. When appropriate the call to run() will be executed.
     * This operation is expected to call releaseRadio() at appropriate point after the run() was called.
     */
    public Observable<T> asObservable() {
        return nextPublishSubject.mergeWith(errorObservable).takeUntil(completePublishSubject);
    }

    @Override
    public final void run() {

        try {
            protectedRun();
        } catch (DeadObjectException deadObjectException) {
            onError(provideException(deadObjectException));
        } catch (Throwable throwable) {
            onError(throwable);
        }
    }

    /**
     * This method will be overridden in concrete operation implementations and
     * will contain specific operation logic.
     */
    protected abstract void protectedRun() throws Throwable;

    /**
     * This function will be overriden in concrete operation implementations to provide an exception with needed context
     * @param deadObjectException the cause for the exception
     */
    protected abstract BleException provideException(DeadObjectException deadObjectException);

    /**
     * A convenience method for getting a representation of the Subscriber
     */
    protected final Subscriber<T> getSubscriber() {
        return new Subscriber<T>() {
            @Override
            public void onCompleted() {
                RxBleRadioOperationReusable.this.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                RxBleRadioOperationReusable.this.onError(e);
            }

            @Override
            public void onNext(T t) {
                RxBleRadioOperationReusable.this.onNext(t);
            }
        };
    }

    /**
     * A convenience method for calling the Subscriber's onNext()
     *
     * @param next the next value
     */
    protected final void onNext(T next) {
        nextPublishSubject.call(next);
    }

    /**
     * A convenience method for calling the Subscriber's onCompleted()
     */
    protected final void onCompleted() {
        completePublishSubject.call(Boolean.TRUE);
    }

    /**
     * A convenience method for calling the Subscriber's onError()
     * Calling this method automatically releases the radio -> calls releaseRadio().
     *
     * @param throwable the throwable
     */
    protected final void onError(Throwable throwable) {
        releaseRadio();
        errorPublishSubject.call(throwable);
    }

    /**
     * The setter for the semaphore which needs to be released for the Bluetooth Radio to continue the work
     *
     * @param radioBlockingSemaphore the semaphore
     */
    public void setRadioBlockingSemaphore(Semaphore radioBlockingSemaphore) {
        this.radioBlockingSemaphore = radioBlockingSemaphore;
    }

    /**
     * A convenience method for releasing the Bluetooth Radio
     * This must be called at appropriate time of the happy-flow
     */
    protected final void releaseRadio() {
        radioBlockingSemaphore.release();
    }

    /**
     * A function returning the priority of this operation
     *
     * @return the priority of this operation
     */
    public RxBleRadioOperation.Priority definedPriority() {
        return RxBleRadioOperation.Priority.NORMAL;
    }

    /**
     * The function for determining which position in Bluetooth Radio's Priority Blocking Queue
     * this operation should take
     *
     * @param another another operation
     * @return the comparison result
     */
    @Override
    public int compareTo(@NonNull Operation another) {
        return another.definedPriority().value() - definedPriority().value();
    }
}
