package com.polidea.rxandroidble

import com.polidea.rxandroidble.internal.RxBleRadio
import com.polidea.rxandroidble.internal.operations.Operation
import rx.Emitter
import rx.Observable
import rx.Scheduler
import rx.Subscription
import rx.functions.Action1
import rx.functions.Cancellable
import rx.schedulers.Schedulers

class FlatRxBleRadio implements RxBleRadio {
    public final MockSemaphore semaphore = new MockSemaphore()

    def Scheduler scheduler() {
        return Schedulers.immediate()
    }

    @Override
    def <T> Observable<T> queue(Operation<T> rxBleRadioOperation) {
        return Observable.create(
                new Action1<Emitter>() {

                    @Override
                    void call(Emitter tEmitter) {
                        Subscription s = rxBleRadioOperation
                                .asObservable()
                                .subscribe(tEmitter)

                        rxBleRadioOperation.setRadioBlockingSemaphore(semaphore)
                        semaphore.acquire()
                        rxBleRadioOperation.run()

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
