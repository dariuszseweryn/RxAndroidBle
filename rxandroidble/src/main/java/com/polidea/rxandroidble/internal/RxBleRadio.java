package com.polidea.rxandroidble.internal;

import rx.Observable;

public interface RxBleRadio {

    <T> Observable<T> queue(RxBleRadioOperation<T> rxBleRadioOperation);
}
