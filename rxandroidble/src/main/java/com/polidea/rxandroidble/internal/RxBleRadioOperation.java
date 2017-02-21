package com.polidea.rxandroidble.internal;

import android.os.DeadObjectException;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.exceptions.BleException;
import java.util.concurrent.Semaphore;

import rx.Observable;
import rx.Subscriber;
import rx.subjects.ReplaySubject;

/**
 * The base class for all operations that are executed on the Bluetooth Radio.
 * This class is intended to be a kind of wrapper over an Observable (returned by function asObservable()).
 * Implements Runnable interface which will be called on the applications main thread.
 *
 * @param <T> What is returned from this operation onNext()
 */
public abstract class RxBleRadioOperation<T> implements Runnable, Comparable<RxBleRadioOperation> {

    /**
     * Operation may start emission even before anyone is looking for it's values. It is safe to replay here as this subject is subscribed
     * only once, after queueing.
     */
    private ReplaySubject<T> replaySubject = ReplaySubject.create();

    private Semaphore radioBlockingSemaphore;

    /**
     * A function that returns this operation as an Observable.
     * When the returned observable will be subscribed this operation will be scheduled
     * to be run on the main thread in future. When appropriate the call to run() will be executed.
     * This operation is expected to call releaseRadio() at appropriate point after the run() was called.
     */
    public Observable<T> asObservable() {
        return replaySubject;
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

    /**
     * A convenience method for calling the Subscriber's onNext()
     *
     * @param next the next value
     */
    protected final void onNext(T next) {
        replaySubject.onNext(next);
    }

    /**
     * A convenience method for calling the Subscriber's onCompleted()
     */
    protected final void onCompleted() {
        replaySubject.onCompleted();
    }

    /**
     * A convenience method for calling the Subscriber's onError()
     * Calling this method automatically releases the radio -> calls releaseRadio().
     *
     * @param throwable the throwable
     */
    protected final void onError(Throwable throwable) {
        releaseRadio();
        replaySubject.onError(throwable);
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
    protected Priority definedPriority() {
        return Priority.NORMAL;
    }

    /**
     * The function for determining which position in Bluetooth Radio's Priority Blocking Queue
     * this operation should take
     *
     * @param another another operation
     * @return the comparison result
     */
    @Override
    public int compareTo(@NonNull RxBleRadioOperation another) {
        return another.definedPriority().priority - definedPriority().priority;
    }

    /**
     * The class representing a priority with which an RxBleRadioOperation should be executed.
     * Used in @Override definedPriority()
     */
    public static class Priority {

        public static final Priority HIGH = new Priority(100);
        public static final Priority NORMAL = new Priority(50);
        public static final Priority LOW = new Priority(0);
        private final int priority;

        private Priority(int priority) {

            this.priority = priority;
        }
    }
}
