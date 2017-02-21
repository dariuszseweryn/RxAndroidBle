package com.polidea.rxandroidble.internal;

import rx.Observable;
import rx.Scheduler;

public interface RxBleRadio {

    Scheduler scheduler();

    <T> Observable<T> queue(RxBleRadioOperation<T> rxBleRadioOperation);
}
