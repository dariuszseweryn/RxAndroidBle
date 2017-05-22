package com.polidea.rxandroidble.internal.radio;

import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.Operation;
import java.util.concurrent.Semaphore;
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
                        final Operation operation = queue.take();
                        log("STARTED", operation);

                        /**
                         * Calling bluetooth calls before the previous one returns in a callback usually finishes with a failure
                         * status. Below Semaphore is passed to the RxBleRadioOperation and is meant to be released at appropriate time
                         * when the next operation should be able to start successfully.
                         */
                        final Semaphore semaphore = new Semaphore(0);

                        operation.setRadioBlockingSemaphore(semaphore);

                        /**
                         * In some implementations (i.e. Samsung Android 4.3) calling BluetoothDevice.connectGatt()
                         * from thread other than main thread ends in connecting with status 133. It's safer to make bluetooth calls
                         * on the main thread.
                         */
                        Observable.just(operation)
                                .observeOn(callbackScheduler)
                                .subscribe(new Action1<Operation>() {
                                    @Override
                                    public void call(Operation operation) {
                                        operation.run();
                                    }
                                });
                        semaphore.acquire();
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
                final Subscription subscription = operation
                        .asObservable()
                        .subscribe(tEmitter);

                queue.add(operation);
                log("QUEUED", operation);

                tEmitter.setCancellation(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        subscription.unsubscribe();
                        if (queue.remove(operation)) {
                            log("REMOVED", operation);
                        }
                    }
                });
            }
        }, Emitter.BackpressureMode.NONE);
    }

    void log(String prefix, Operation rxBleRadioOperation) {

        if (RxBleLog.isAtLeast(RxBleLog.DEBUG)) {
            RxBleLog.d("%8s %s(%d)", prefix, rxBleRadioOperation.getClass().getSimpleName(), System.identityHashCode(rxBleRadioOperation));
        }
    }
}
