package com.polidea.rxandroidble.internal.serialization;

import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.operations.Operation;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Emitter;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Cancellable;

public class ConnectionOperationQueueImpl implements ConnectionOperationQueue {

    private OperationPriorityFifoBlockingQueue queue = new OperationPriorityFifoBlockingQueue();
    private final Future<?> runnableFuture;
    private volatile boolean shouldRun = true;
    private volatile BleDisconnectedException bleDisconnectedException = null;

    @Inject
    public ConnectionOperationQueueImpl(
            @Named(ClientComponent.NamedExecutors.CONNECTION_QUEUE) final ExecutorService executorService,
            @Named(ClientComponent.NamedSchedulers.BLUETOOTH_INTERACTION) final Scheduler callbackScheduler
    ) {
        runnableFuture = executorService.submit(new Runnable() {
            @Override
            public void run() {
                QueueSemaphore currentSemaphore;
                while (shouldRun) {
                    try {
                        final FIFORunnableEntry<?> entry  = queue.take();
                        final Operation<?> operation = entry.operation;
                        log("STARTED", operation);

                        /*
                         * Calling bluetooth calls before the previous one returns in a callback usually finishes with a failure
                         * status. Below QueueSemaphore is passed to the Operation and is meant to be released
                         * at appropriate time when the next operation should be able to start successfully.
                         */
                        currentSemaphore = new QueueSemaphore();

                        Subscription subscription = entry.run(currentSemaphore, callbackScheduler);
                        entry.emitter.setSubscription(subscription);

                        currentSemaphore.awaitRelease();
                        log("FINISHED", operation);
                    } catch (InterruptedException e) {
                        synchronized (this) {
                            if (!shouldRun) {
                                break;
                            }
                        }
                        RxBleLog.e(e, "Error while processing connection operation queue");
                    }
                }

                flushQueue();
                RxBleLog.d("Terminated.");
            }
        });
    }

    private synchronized void flushQueue() {
        while (!queue.isEmpty()) {
            final FIFORunnableEntry<?> entryToFinish = queue.takeNow();
            entryToFinish.emitter.onError(bleDisconnectedException);
        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public synchronized <T> Observable<T> queue(final Operation<T> operation) {
        if (!shouldRun) {
            return Observable.error(bleDisconnectedException);
        }
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

    @Override
    public synchronized void terminate(BleDisconnectedException disconnectionException) {
        RxBleLog.i("Connection operations queue to be terminated (" + disconnectionException.bluetoothDeviceAddress + ')');
        shouldRun = false;
        bleDisconnectedException = disconnectionException;
        runnableFuture.cancel(true);
    }

    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    void log(String prefix, Operation operation) {

        if (RxBleLog.isAtLeast(RxBleLog.DEBUG)) {
            RxBleLog.d("%8s %s(%d)", prefix, operation.getClass().getSimpleName(), System.identityHashCode(operation));
        }
    }
}
