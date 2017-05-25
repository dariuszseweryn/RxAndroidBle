package com.polidea.rxandroidble.internal.radio;

import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.Operation;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Emitter;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Cancellable;

public class RxBleRadioImpl implements RxBleRadio {

    private OperationPriorityFifoBlockingQueue queue = new OperationPriorityFifoBlockingQueue();

    @Inject
    public RxBleRadioImpl(@Named(ClientComponent.NamedSchedulers.RADIO_OPERATIONS) final Scheduler callbackScheduler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //noinspection InfiniteLoopStatement
                while (true) {
                    try {
                        final FIFORunnableEntry<?> entry = queue.take();
                        final Operation<?> operation = entry.operation;
                        log("STARTED", operation);

                        /*
                         * Calling bluetooth calls before the previous one returns in a callback usually finishes with a failure
                         * status. Below RadioSynchronizationInterface is passed to the RxBleRadioOperation and is meant to be released
                         * at appropriate time when the next operation should be able to start successfully.
                         */
                        final RadioSemaphore radioSemaphore = new RadioSemaphore();

                        Subscription subscription = entry.run(radioSemaphore, callbackScheduler);
                        entry.emitter.setSubscription(subscription);

                        radioSemaphore.awaitRelease();
                        log("FINISHED", operation);
                    } catch (InterruptedException e) {
                        RxBleLog.e(e, "Error while processing RxBleRadioOperation queue");
                    }
                }
            }
        }).start();
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public <T> Observable<T> queue(final Operation<T> operation) {
        return Observable.create(new Action1<Emitter<T>>() {
            @Override
            public void call(Emitter<T> tEmitter) {
                final FIFORunnableEntry entry = new FIFORunnableEntry<>(operation, tEmitter);

                tEmitter.setCancellation(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        if (queue.remove(entry)) {
                            log("REMOVED", operation);
                        }
                    }
                });

                log("QUEUED", operation);
                queue.add(entry);
            }
        }, Emitter.BackpressureMode.NONE);
    }

    void log(String prefix, Operation rxBleRadioOperation) {

        if (RxBleLog.isAtLeast(RxBleLog.DEBUG)) {
            RxBleLog.d("%8s %s(%d)", prefix, rxBleRadioOperation.getClass().getSimpleName(), System.identityHashCode(rxBleRadioOperation));
        }
    }
}
