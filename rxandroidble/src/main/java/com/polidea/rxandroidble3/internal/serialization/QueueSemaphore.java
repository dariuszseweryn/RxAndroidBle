package com.polidea.rxandroidble3.internal.serialization;


import com.polidea.rxandroidble3.internal.RxBleLog;
import java.util.concurrent.atomic.AtomicBoolean;

class QueueSemaphore implements QueueReleaseInterface, QueueAwaitReleaseInterface {

    private final AtomicBoolean isReleased = new AtomicBoolean(false);

    @Override
    public synchronized void awaitRelease() throws InterruptedException {
        while (!isReleased.get()) {
            try {
                wait();
            } catch (InterruptedException e) {
                if (!isReleased.get()) {
                    RxBleLog.w(e, "Queue's awaitRelease() has been interrupted abruptly "
                            + "while it wasn't released by the release() method.");
                }
            }
        }
    }

    @Override
    public synchronized void release() {
        if (isReleased.compareAndSet(false, true)) {
            notify();
        }
    }
}
