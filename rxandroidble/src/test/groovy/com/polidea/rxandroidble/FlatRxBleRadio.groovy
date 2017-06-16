package com.polidea.rxandroidble

import com.polidea.rxandroidble.internal.RxBleRadio
import com.polidea.rxandroidble.internal.operations.Operation
import rx.Emitter
import rx.Observable
import rx.Subscription
import rx.functions.Action1
import rx.functions.Cancellable

class FlatRxBleRadio implements RxBleRadio {
    public final MockSemaphore semaphore = new MockSemaphore()

    @Override
    def <T> Observable<T> queue(Operation<T> operation) {
        return Observable.create(
                new Action1<Emitter>() {

                    @Override
                    void call(Emitter tEmitter) {
                        semaphore.awaitRelease()

                        Subscription s = operation
                                .run(semaphore)
                                .subscribe(tEmitter)

                        tEmitter.setCancellation(new Cancellable() {

                            @Override
                            void cancel() throws Exception {
                                s.unsubscribe();
                            }
                        })
                    }
                },
                Emitter.BackpressureMode.NONE
        )
    }
}
