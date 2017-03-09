package com.polidea.rxandroidble.internal.radio;

import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;

import java.util.concurrent.Semaphore;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;

public class RxBleRadioImpl implements RxBleRadio {

    private OperationPriorityFifoBlockingQueue queue = new OperationPriorityFifoBlockingQueue();
    private final Scheduler scheduler;

    public RxBleRadioImpl(final Scheduler scheduler) {
        this.scheduler = scheduler;

        new Thread(new Runnable() {
            @Override
            public void run() {
                //noinspection InfiniteLoopStatement
                while (true) {
                    try {
                        final RxBleRadioOperation rxBleRadioOperation = queue.take();
                        RxBleRadioImpl.this.log("STARTED", rxBleRadioOperation);

                        /**
                         * Calling bluetooth calls before the previous one returns in a callback usually finishes with a failure
                         * status. Below Semaphore is passed to the RxBleRadioOperation and is meant to be released at appropriate time
                         * when the next operation should be able to start successfully.
                         */
                        final Semaphore semaphore = new Semaphore(0);

                        rxBleRadioOperation.setRadioBlockingSemaphore(semaphore);

                        Observable.just(rxBleRadioOperation)
                                .observeOn(RxBleRadioImpl.this.scheduler)
                                .subscribe(new Action1<RxBleRadioOperation>() {
                                    @Override
                                    public void call(RxBleRadioOperation rxBleRadioOperation1) {
                                        rxBleRadioOperation1.run();
                                    }
                                });
                        semaphore.acquire();
                        log("FINISHED", rxBleRadioOperation);
                    } catch (InterruptedException e) {
                        RxBleLog.e(e, "Error while processing RxBleRadioOperation queue");
                    }
                }
            }
        }).start();
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public <T> Observable<T> queue(final RxBleRadioOperation<T> rxBleRadioOperation) {
        return rxBleRadioOperation
                .asObservable()
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        RxBleRadioImpl.this.log("QUEUED", rxBleRadioOperation);
                        queue.add(rxBleRadioOperation);
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        if (queue.remove(rxBleRadioOperation)) {
                            RxBleRadioImpl.this.log("REMOVED", rxBleRadioOperation);
                        }
                    }
                });
    }

    void log(String prefix, RxBleRadioOperation rxBleRadioOperation) {

        if (RxBleLog.isAtLeast(RxBleLog.DEBUG)) {
            RxBleLog.d("%8s %s(%d)", prefix, rxBleRadioOperation.getClass().getSimpleName(), System.identityHashCode(rxBleRadioOperation));
        }
    }
}
