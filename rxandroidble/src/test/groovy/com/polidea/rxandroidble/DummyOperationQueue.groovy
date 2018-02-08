package com.polidea.rxandroidble

import com.polidea.rxandroidble.exceptions.BleException
import com.polidea.rxandroidble.internal.operations.Operation
import com.polidea.rxandroidble.internal.serialization.ConnectionOperationQueue
import com.polidea.rxandroidble.internal.util.DisposableUtil
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.annotations.NonNull

class DummyOperationQueue implements ConnectionOperationQueue {
    public final MockSemaphore semaphore = new MockSemaphore()

    @Override
    def <T> Observable<T> queue(Operation<T> operation) {
        return Observable.create(new ObservableOnSubscribe() {
            @Override
            void subscribe(@NonNull ObservableEmitter tEmitter) throws Exception {
                semaphore.awaitRelease()
                def disposableObserver = operation
                        .run(semaphore)
                        .subscribeWith(DisposableUtil.disposableEmitter(tEmitter))
                tEmitter.setDisposable(disposableObserver)
            }
        })
    }

    @Override
    void terminate(BleException disconnectException) {
        // do nothing
    }
}
