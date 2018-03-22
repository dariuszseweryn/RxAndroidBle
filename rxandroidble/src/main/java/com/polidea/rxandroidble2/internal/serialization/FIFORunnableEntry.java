package com.polidea.rxandroidble2.internal.serialization;


import android.support.annotation.NonNull;

import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.internal.operations.Operation;

import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.ObservableEmitter;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;

class FIFORunnableEntry<T> implements Comparable<FIFORunnableEntry> {

    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    private final long seqNum;
    final Operation<T> operation;
    final ObservableEmitter<T> operationResultObserver;

    FIFORunnableEntry(Operation<T> operation, ObservableEmitter<T> operationResultObserver) {
        seqNum = SEQUENCE.getAndIncrement();
        this.operation = operation;
        this.operationResultObserver = operationResultObserver;
    }

    public int compareTo(@NonNull FIFORunnableEntry other) {
        int res = operation.compareTo(other.operation);
        if (res == 0 && other.operation != this.operation) {
            res = (seqNum < other.seqNum ? -1 : 1);
        }
        return res;
    }

    public void run(QueueSemaphore semaphore, Scheduler subscribeScheduler) {
        /*
         * In some implementations (i.e. Samsung Android 4.3) calling BluetoothDevice.connectGatt()
         * from thread other than main thread ends in connecting with status 133. It's safer to make bluetooth calls
         * on the main thread.
         */

        if (!operationResultObserver.isDisposed()) {
            operation.run(semaphore)
                    .subscribeOn(subscribeScheduler)
                    .unsubscribeOn(subscribeScheduler)
                    .subscribe(new Observer<T>() {
                        @Override
                        public void onSubscribe(Disposable disposable) {
                            /*
                             * We end up overwriting a disposable that was set to the observer in order to remove operation from queue.
                             * This is ok since at this moment the operation is taken out of the queue anyway.
                             */
                            operationResultObserver.setDisposable(disposable);
                        }

                        @Override
                        public void onNext(T item) {
                            operationResultObserver.onNext(item);
                        }

                        @Override
                        public void onError(Throwable e) {
                            operationResultObserver.tryOnError(e);
                        }

                        @Override
                        public void onComplete() {
                            operationResultObserver.onComplete();
                        }
                    });
        } else {
            RxBleLog.d("FIFORunnableEntry", "Operation was about to be run but "
                    + "the observer was already disposed: " + operation);
        }
    }
}
