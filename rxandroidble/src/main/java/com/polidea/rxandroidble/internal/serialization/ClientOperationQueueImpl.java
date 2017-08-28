package com.polidea.rxandroidble.internal.serialization;

import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.operations.Operation;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Emitter;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Cancellable;

public class ClientOperationQueueImpl implements ClientOperationQueue {

    private OperationPriorityFifoBlockingQueue queue = new OperationPriorityFifoBlockingQueue();

    @Inject
    public ClientOperationQueueImpl(@Named(ClientComponent.NamedSchedulers.BLUETOOTH_INTERACTION) final Scheduler callbackScheduler) {
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
                         * status. Below a QueueSemaphore is passed to the RxBleCustomOperation and is meant to be released
                         * at appropriate time when the next operation should be able to start successfully.
                         */
                        final QueueSemaphore clientOperationSemaphore = new QueueSemaphore();

                        Subscription subscription = entry.run(clientOperationSemaphore, callbackScheduler);
                        entry.emitter.setSubscription(subscription);

                        clientOperationSemaphore.awaitRelease();
                        log("FINISHED", operation);
                    } catch (InterruptedException e) {
                        RxBleLog.e(e, "Error while processing client operation queue");
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

    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    void log(String prefix, Operation operation) {

        if (RxBleLog.isAtLeast(RxBleLog.DEBUG)) {
            RxBleLog.d("%8s %s(%d)", prefix, operation.getClass().getSimpleName(), System.identityHashCode(operation));
        }
    }
}
