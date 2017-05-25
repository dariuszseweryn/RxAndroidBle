package com.polidea.rxandroidble.internal;

import com.polidea.rxandroidble.internal.operations.Operation;
import rx.Observable;

public interface RxBleRadio {

    <T> Observable<T> queue(Operation<T> operation);
}
