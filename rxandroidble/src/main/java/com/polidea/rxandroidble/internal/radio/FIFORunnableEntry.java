package com.polidea.rxandroidble.internal.radio;


import android.support.annotation.NonNull;
import com.polidea.rxandroidble.internal.operations.Operation;
import java.util.concurrent.atomic.AtomicLong;
import rx.Emitter;
import rx.Scheduler;
import rx.Subscription;

class FIFORunnableEntry<T> implements Comparable<FIFORunnableEntry> {

    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    private final long seqNum;

    final Operation<T> operation;

    final Emitter<T> emitter;

    FIFORunnableEntry(Operation<T> operation, Emitter<T> subject) {
        seqNum = SEQUENCE.getAndIncrement();
        this.operation = operation;
        this.emitter = subject;
    }

    public int compareTo(@NonNull FIFORunnableEntry other) {
        int res = operation.compareTo(other.operation);
        if (res == 0 && other.operation != this.operation) {
            res = (seqNum < other.seqNum ? -1 : 1);
        }
        return res;
    }

    public Subscription run(RadioSemaphore semaphore, Scheduler subscribeScheduler) {
        /*
         * In some implementations (i.e. Samsung Android 4.3) calling BluetoothDevice.connectGatt()
         * from thread other than main thread ends in connecting with status 133. It's safer to make bluetooth calls
         * on the main thread.
         */
        return operation.run(semaphore)
                .subscribeOn(subscribeScheduler)
                .unsubscribeOn(subscribeScheduler)
                .subscribe(emitter);
    }
}
