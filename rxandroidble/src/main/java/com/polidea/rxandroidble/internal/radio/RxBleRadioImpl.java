package com.polidea.rxandroidble.internal.radio;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;

import java.util.concurrent.Semaphore;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;

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
                        final RxBleRadioOperation rxBleRadioOperation = queue.take();
                        log("STARTED", rxBleRadioOperation);

                        /**
                         * Calling bluetooth calls before the previous one returns in a callback usually finishes with a failure
                         * status. Below Semaphore is passed to the RxBleRadioOperation and is meant to be released at appropriate time
                         * when the next operation should be able to start successfully.
                         */
                        final Semaphore semaphore = new Semaphore(0);

                        rxBleRadioOperation.setRadioBlockingSemaphore(semaphore);

                        /**
                         * In some implementations (i.e. Samsung Android 4.3) calling BluetoothDevice.connectGatt()
                         * from thread other than main thread ends in connecting with status 133. It's safer to make bluetooth calls
                         * on the main thread.
                         */
                        Observable.just(rxBleRadioOperation)
                                .observeOn(callbackScheduler)
                                .subscribe(new Action1<RxBleRadioOperation>() {
                                    @Override
                                    public void call(RxBleRadioOperation operation) {
                                        operation.run();
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
    public <T> Observable<T> queue(final RxBleRadioOperation<T> rxBleRadioOperation) {
        return rxBleRadioOperation
                .asObservable()
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        log("QUEUED", rxBleRadioOperation);
                        queue.add(rxBleRadioOperation);
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        if (queue.remove(rxBleRadioOperation)) {
                            log("REMOVED", rxBleRadioOperation);
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
