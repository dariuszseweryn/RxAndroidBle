package com.polidea.rxandroidble.internal.radio;


import com.polidea.rxandroidble.internal.RadioAwaitReleaseInterface;
import com.polidea.rxandroidble.internal.RadioReleaseInterface;
import com.polidea.rxandroidble.internal.RxBleLog;
import java.util.concurrent.atomic.AtomicBoolean;

class RadioSemaphore implements RadioReleaseInterface, RadioAwaitReleaseInterface {

    private final AtomicBoolean isReleased = new AtomicBoolean(false);

    @Override
    public synchronized void awaitRelease() throws InterruptedException {
        while (!isReleased.get()) {
            try {
                wait();
            } catch (InterruptedException e) {
                RxBleLog.v(e, "Interrupted awaitRelease()");
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
