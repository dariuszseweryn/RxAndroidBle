package com.polidea.rxandroidble.internal;

import android.util.Log;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import rx.Observable;

public class RxBleRadioImpl implements RxBleRadio {

    private static final String TAG = RxBleRadioImpl.class.getSimpleName();

    private BlockingQueue<RxBleRadioOperation> queue = new PriorityBlockingQueue<>();

    public RxBleRadioImpl() {
        new Thread(() -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    final RxBleRadioOperation rxBleRadioOperation = queue.take();
                    log("STARTED", rxBleRadioOperation);

                    final Semaphore semaphore = new Semaphore(0);

                    rxBleRadioOperation.setRadioBlockingSemaphore(semaphore);
                    rxBleRadioOperation.run();

                    semaphore.acquire();
                    log("FINISHED", rxBleRadioOperation);
                } catch (InterruptedException e) {
                    Log.e(getClass().getSimpleName(), "Error while processing RxBleRadioOperation queue", e); // FIXME: introduce Timber?
                }
            }
        }).start();
    }


    @Override
    public <T> Observable<T> queue(RxBleRadioOperation<T> rxBleRadioOperation) {
        return Observable.create(subscriber -> {
            log("QUEUED", rxBleRadioOperation);
            rxBleRadioOperation.asObservable().subscribe(subscriber);
            queue.add(rxBleRadioOperation);
        });
    }

    private void log(String prefix, RxBleRadioOperation rxBleRadioOperation) {
        Log.d(TAG, prefix + " " + rxBleRadioOperation.getClass().getSimpleName() + "(" + System.identityHashCode(rxBleRadioOperation) + ")");
    }
}
