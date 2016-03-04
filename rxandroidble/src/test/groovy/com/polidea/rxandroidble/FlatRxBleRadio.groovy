package com.polidea.rxandroidble

import com.polidea.rxandroidble.internal.RxBleRadio
import com.polidea.rxandroidble.internal.RxBleRadioOperation
import rx.Observable

class FlatRxBleRadio implements RxBleRadio {
    public final MockSemaphore semaphore = new MockSemaphore()

    @Override
    def <T> Observable<T> queue(RxBleRadioOperation<T> rxBleRadioOperation) {
        return rxBleRadioOperation
                .asObservable()
                .doOnSubscribe({
            rxBleRadioOperation.setRadioBlockingSemaphore(semaphore)
            semaphore.acquire()
            rxBleRadioOperation.run()
        })
    }
}
