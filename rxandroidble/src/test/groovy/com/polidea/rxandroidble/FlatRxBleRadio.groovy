package com.polidea.rxandroidble

import com.polidea.rxandroidble.internal.RxBleRadio
import com.polidea.rxandroidble.internal.operations.Operation
import rx.Observable

class FlatRxBleRadio implements RxBleRadio {
    public final MockSemaphore semaphore = new MockSemaphore()

    @Override
    def <T> Observable<T> queue(Operation<T> operation) {
        return operation.run(semaphore)
    }
}
