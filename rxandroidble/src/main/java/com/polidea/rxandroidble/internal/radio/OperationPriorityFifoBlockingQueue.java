package com.polidea.rxandroidble.internal.radio;


import java.util.concurrent.PriorityBlockingQueue;

class OperationPriorityFifoBlockingQueue {

    private final PriorityBlockingQueue<FIFORunnableEntry> q = new PriorityBlockingQueue<>();

    public void add(FIFORunnableEntry fifoRunnableEntry) {
        q.add(fifoRunnableEntry);
    }

    public FIFORunnableEntry<?> take() throws InterruptedException {
        return q.take();
    }

    public boolean isEmpty() {
        return q.isEmpty();
    }

    public boolean remove(FIFORunnableEntry fifoRunnableEntry) {
        for (FIFORunnableEntry entry : q) {
            if (entry == fifoRunnableEntry) {
                return q.remove(entry);
            }
        }
        return false;
    }
}
