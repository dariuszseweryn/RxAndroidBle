package com.polidea.rxandroidble3

import com.polidea.rxandroidble3.exceptions.BleException
import com.polidea.rxandroidble3.internal.operations.Operation
import com.polidea.rxandroidble3.internal.serialization.ConnectionOperationQueue
import com.polidea.rxandroidble3.internal.util.DisposableUtil
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe
import io.reactivex.rxjava3.annotations.NonNull

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
                        .subscribeWith(DisposableUtil.disposableObserverFromEmitter(tEmitter))
                tEmitter.setDisposable(disposableObserver)
            }
        })
    }

    @Override
    void terminate(BleException disconnectException) {
        // do nothing
    }
}
