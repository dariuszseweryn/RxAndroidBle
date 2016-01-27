package com.polidea.rxandroidble.internal;

import android.util.Log;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class RxBleRadioImpl implements RxBleRadio {

    private BlockingQueue<RxBleRadioOperation> queue = new LinkedBlockingQueue<>();

    public RxBleRadioImpl() {
        new Thread(() -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    final RxBleRadioOperation rxBleRadioOperation = queue.take();
                    Log.d("RxBleRadioImpl", "Running " + rxBleRadioOperation.getClass().getSimpleName());

                    final Semaphore semaphore = new Semaphore(0);

                    rxBleRadioOperation.setRadioBlockingSemaphore(semaphore);
                    rxBleRadioOperation.run();

                    semaphore.acquire();
                    Log.d("RxBleRadioImpl", "Finished " + rxBleRadioOperation.getClass().getSimpleName());
                } catch (InterruptedException e) {
                    Log.e(getClass().getSimpleName(), "Error while processing RxBleRadioOperation queue", e); // FIXME: introduce Timber?
                }
            }
        }).start();
    }

    @Override
    public void queue(RxBleRadioOperation rxBleRadioOperation) {
        queue.add(rxBleRadioOperation);
    }
}
