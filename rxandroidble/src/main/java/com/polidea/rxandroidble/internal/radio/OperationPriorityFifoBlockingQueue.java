package com.polidea.rxandroidble.internal.radio;

import android.support.annotation.NonNull;

import com.polidea.rxandroidble.internal.operations.Operation;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

class OperationPriorityFifoBlockingQueue {

    private final PriorityBlockingQueue<FIFOEntry> q = new PriorityBlockingQueue<>();

    public void add(Operation object) {
        q.add(new FIFOEntry(object));
    }

    public Operation take() throws InterruptedException {
        return q.take().getEntry();
    }

    public boolean isEmpty() {
        return q.isEmpty();
    }

    public boolean remove(Operation rxBleRadioOperation) {
        for (FIFOEntry entry : q) {
            if (entry.getEntry() == rxBleRadioOperation) {
                return q.remove(entry);
            }
        }
        return false;
    }

    private static class FIFOEntry implements Comparable<FIFOEntry> {

        static final AtomicLong SEQUENCE = new AtomicLong(0);

        final long seqNum;

        final Operation<?> entry;

        FIFOEntry(Operation entry) {
            seqNum = SEQUENCE.getAndIncrement();
            this.entry = entry;
        }

        Operation getEntry() {
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
