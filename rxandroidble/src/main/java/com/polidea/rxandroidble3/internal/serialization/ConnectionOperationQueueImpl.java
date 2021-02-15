package com.polidea.rxandroidble3.internal.serialization;

import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble3.ClientComponent;
import com.polidea.rxandroidble3.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble3.exceptions.BleException;
import com.polidea.rxandroidble3.internal.DeviceModule;
import com.polidea.rxandroidble3.internal.RxBleLog;
import com.polidea.rxandroidble3.internal.connection.ConnectionScope;
import com.polidea.rxandroidble3.internal.connection.ConnectionSubscriptionWatcher;
import com.polidea.rxandroidble3.internal.connection.DisconnectionRouterOutput;
import com.polidea.rxandroidble3.internal.operations.Operation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.functions.Cancellable;
import io.reactivex.rxjava3.observers.DisposableObserver;

import static com.polidea.rxandroidble3.internal.logger.LoggerUtil.commonMacMessage;
import static com.polidea.rxandroidble3.internal.logger.LoggerUtil.logOperationFinished;
import static com.polidea.rxandroidble3.internal.logger.LoggerUtil.logOperationQueued;
import static com.polidea.rxandroidble3.internal.logger.LoggerUtil.logOperationRemoved;
import static com.polidea.rxandroidble3.internal.logger.LoggerUtil.logOperationRunning;
import static com.polidea.rxandroidble3.internal.logger.LoggerUtil.logOperationStarted;

@ConnectionScope
public class ConnectionOperationQueueImpl implements ConnectionOperationQueue, ConnectionSubscriptionWatcher {

    private final String deviceMacAddress;
    private final DisconnectionRouterOutput disconnectionRouterOutput;
    private DisposableObserver<BleException> disconnectionThrowableSubscription;
    final OperationPriorityFifoBlockingQueue queue = new OperationPriorityFifoBlockingQueue();
    private final Future<?> runnableFuture;
    volatile boolean shouldRun = true;
    private BleException disconnectionException = null;

    @Inject
    ConnectionOperationQueueImpl(
            @Named(DeviceModule.MAC_ADDRESS) final String deviceMacAddress,
            final DisconnectionRouterOutput disconnectionRouterOutput,
            @Named(ClientComponent.NamedExecutors.CONNECTION_QUEUE) final ExecutorService executorService,
            @Named(ClientComponent.NamedSchedulers.BLUETOOTH_INTERACTION) final Scheduler callbackScheduler
    ) {
        this.deviceMacAddress = deviceMacAddress;
        this.disconnectionRouterOutput = disconnectionRouterOutput;
        this.runnableFuture = executorService.submit(new Runnable() {
            @Override
            public void run() {
                QueueSemaphore currentSemaphore;
                while (shouldRun) {
                    try {
                        final FIFORunnableEntry<?> entry = queue.take();
                        final Operation<?> operation = entry.operation;
                        final long startedAtTime = System.currentTimeMillis();
                        logOperationStarted(operation);
                        logOperationRunning(operation);

                        /*
                         * Calling bluetooth calls before the previous one returns in a callback usually finishes with a failure
                         * status. Below QueueSemaphore is passed to the Operation and is meant to be released
                         * at appropriate time when the next operation should be able to start successfully.
                         */
                        currentSemaphore = new QueueSemaphore();
                        entry.run(currentSemaphore, callbackScheduler);
                        currentSemaphore.awaitRelease();
                        logOperationFinished(operation, startedAtTime, System.currentTimeMillis());
                    } catch (InterruptedException e) {
                        synchronized (ConnectionOperationQueueImpl.this) {
                            if (!shouldRun) {
                                break;
                            }
                        }
                        RxBleLog.e(e, "Error while processing connection operation queue");
                    }
                }

                flushQueue();
                RxBleLog.v("Terminated (%s)", commonMacMessage(deviceMacAddress));
            }
        });
    }

    synchronized void flushQueue() {
        while (!queue.isEmpty()) {
            final FIFORunnableEntry<?> entryToFinish = queue.takeNow();
            entryToFinish.operationResultObserver.tryOnError(disconnectionException);
        }
    }

    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public synchronized <T> Observable<T> queue(final Operation<T> operation) {
        if (!shouldRun) {
            return Observable.error(disconnectionException);
        }

        return Observable.create(new ObservableOnSubscribe<T>() {
            @Override
            public void subscribe(ObservableEmitter<T> emitter) {
                final FIFORunnableEntry entry = new FIFORunnableEntry<>(operation, emitter);
                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() {
                        if (queue.remove(entry)) {
                            logOperationRemoved(operation);
                        }
                    }
                });

                logOperationQueued(operation);
                queue.add(entry);
            }
        });
    }

    @Override
    public synchronized void terminate(BleException disconnectException) {
        if (this.disconnectionException != null) {
            // already terminated
            return;
        }
        RxBleLog.d(disconnectException, "Connection operations queue to be terminated (%s)", commonMacMessage(deviceMacAddress));
        shouldRun = false;
        disconnectionException = disconnectException;
        runnableFuture.cancel(true);
    }

    @Override
    public void onConnectionSubscribed() {
        disconnectionThrowableSubscription = disconnectionRouterOutput.asValueOnlyObservable()
                .subscribeWith(new DisposableObserver<BleException>() {
                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onNext(BleException bleException) {
                        terminate(bleException);
                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }
                });
    }

    @Override
    public void onConnectionUnsubscribed() {
        disconnectionThrowableSubscription.dispose();
        disconnectionThrowableSubscription = null;
        terminate(new BleDisconnectedException(deviceMacAddress, BleDisconnectedException.UNKNOWN_STATUS));
    }
}
