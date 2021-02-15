package com.polidea.rxandroidble3.internal.serialization;

import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble3.ClientComponent;
import com.polidea.rxandroidble3.internal.RxBleLog;
import com.polidea.rxandroidble3.internal.operations.Operation;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import static com.polidea.rxandroidble3.internal.logger.LoggerUtil.logOperationFinished;
import static com.polidea.rxandroidble3.internal.logger.LoggerUtil.logOperationQueued;
import static com.polidea.rxandroidble3.internal.logger.LoggerUtil.logOperationRemoved;
import static com.polidea.rxandroidble3.internal.logger.LoggerUtil.logOperationRunning;
import static com.polidea.rxandroidble3.internal.logger.LoggerUtil.logOperationStarted;

public class ClientOperationQueueImpl implements ClientOperationQueue {

    final OperationPriorityFifoBlockingQueue queue = new OperationPriorityFifoBlockingQueue();

    @Inject
    public ClientOperationQueueImpl(@Named(ClientComponent.NamedSchedulers.BLUETOOTH_INTERACTION) final Scheduler callbackScheduler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        final FIFORunnableEntry<?> entry = queue.take();
                        final Operation<?> operation = entry.operation;
                        final long startedAtTime = System.currentTimeMillis();
                        logOperationStarted(operation);
                        logOperationRunning(operation);

                        /*
                         * Calling bluetooth calls before the previous one returns in a callback usually finishes with a failure
                         * status. Below a QueueSemaphore is passed to the RxBleCustomOperation and is meant to be released
                         * at appropriate time when the next operation should be able to start successfully.
                         */
                        final QueueSemaphore clientOperationSemaphore = new QueueSemaphore();
                        entry.run(clientOperationSemaphore, callbackScheduler);
                        clientOperationSemaphore.awaitRelease();
                        logOperationFinished(operation, startedAtTime, System.currentTimeMillis());
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
        return Observable.create(new ObservableOnSubscribe<T>() {
            @Override
            public void subscribe(ObservableEmitter<T> tEmitter) {
                final FIFORunnableEntry entry = new FIFORunnableEntry<>(operation, tEmitter);

                tEmitter.setDisposable(Disposable.fromAction(new Action() {
                    @Override
                    public void run() {
                        if (queue.remove(entry)) {
                            logOperationRemoved(operation);
                        }
                    }
                }));

                logOperationQueued(operation);
                queue.add(entry);
            }
        });
    }
}
