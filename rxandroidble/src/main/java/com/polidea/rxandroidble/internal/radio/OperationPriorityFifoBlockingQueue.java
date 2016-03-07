package com.polidea.rxandroidble.internal.radio;

import android.support.annotation.NonNull;

import com.polidea.rxandroidble.internal.RxBleRadioOperation;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

class OperationPriorityFifoBlockingQueue {

    private final PriorityBlockingQueue<FIFOEntry> q = new PriorityBlockingQueue<>();

    public void add(RxBleRadioOperation object) {
        q.add(new FIFOEntry(object));
    }

    public RxBleRadioOperation take() throws InterruptedException {
        return q.take().getEntry();
    }

    public boolean isEmpty() {
        return q.isEmpty();
    }

    public void remove(RxBleRadioOperation rxBleRadioOperation) {
        for (FIFOEntry entry : q) {
            if (entry.getEntry() == rxBleRadioOperation) {
                q.remove(entry);
                return;
            }
        }
    }

    private static class FIFOEntry implements Comparable<FIFOEntry> {

        static final AtomicLong seq = new AtomicLong(0);

        final long seqNum;

        final RxBleRadioOperation entry;

        public FIFOEntry(RxBleRadioOperation entry) {
            seqNum = seq.getAndIncrement();
            this.entry = entry;
        }

        public RxBleRadioOperation getEntry() {
            return entry;
        }

        public int compareTo(@NonNull FIFOEntry other) {
            int res = entry.compareTo(other.entry);
            if (res == 0 && other.entry != this.entry) {
                res = (seqNum < other.seqNum ? -1 : 1);
            }
            return res;
        }
    }
}
