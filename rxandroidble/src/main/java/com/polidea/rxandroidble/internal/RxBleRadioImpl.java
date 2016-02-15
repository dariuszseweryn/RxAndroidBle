package com.polidea.rxandroidble.internal;

import android.util.Log;
import java.util.concurrent.Semaphore;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class RxBleRadioImpl implements RxBleRadio {

    private static final String TAG = RxBleRadioImpl.class.getSimpleName();

    private OperationPriorityFifoBlockingQueue queue = new OperationPriorityFifoBlockingQueue();

    public RxBleRadioImpl() {
        new Thread(() -> {
            //noinspection InfiniteLoopStatement
            while (true) {
                try {
                    final RxBleRadioOperation rxBleRadioOperation = queue.take();
                    log("STARTED", rxBleRadioOperation);

                    /**
                     * Calling bluetooth calls before the previous one returns in a callback usually finishes with a failure
                     * status. Below Semaphore is passed to the RxBleRadioOperation and is meant to be released at appropriate time
                     * when the next operation should be able to start successfully.
                     */
                    final Semaphore semaphore = new Semaphore(0);

                    rxBleRadioOperation.setRadioBlockingSemaphore(semaphore);

                    /**
                     * In some implementations (i.e. Samsung Android 4.3) calling BluetoothDevice.connectGatt()
                     * from thread other than main thread ends in connecting with status 133. It's safer to make bluetooth calls
                     * on the main thread.
                     */
                    Observable.just(rxBleRadioOperation)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(Runnable::run);

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
        final Observable<T> observable = Observable.create(subscriber -> {
            log("QUEUED", rxBleRadioOperation);
            rxBleRadioOperation.asObservable().subscribe(subscriber);
            queue.add(rxBleRadioOperation);
        });
        return observable
                .doOnUnsubscribe(() -> queue.remove(rxBleRadioOperation));
    }

    private void log(String prefix, RxBleRadioOperation rxBleRadioOperation) {
        Log.d(TAG, prefix + " " + rxBleRadioOperation.getClass().getSimpleName() + "(" + System.identityHashCode(rxBleRadioOperation) + ")");
    }
}
