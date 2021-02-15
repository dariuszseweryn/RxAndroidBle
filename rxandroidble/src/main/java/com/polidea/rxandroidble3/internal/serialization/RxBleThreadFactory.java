package com.polidea.rxandroidble3.internal.serialization;

import io.reactivex.rxjava3.internal.schedulers.NonBlockingThread;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;


public class RxBleThreadFactory extends AtomicLong implements ThreadFactory {

    @Override
    public Thread newThread(Runnable r) {
        String name = "RxBleThread-" + incrementAndGet();
        Thread t = new RxBleNonBlockingThread(r, name);
        t.setPriority(Thread.NORM_PRIORITY);
        t.setDaemon(true);
        return t;
    }

    static final class RxBleNonBlockingThread extends Thread implements NonBlockingThread {
        RxBleNonBlockingThread(Runnable run, String name) {
            super(run, name);
        }
    }
}
