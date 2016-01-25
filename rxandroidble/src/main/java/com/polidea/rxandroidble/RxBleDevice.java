package com.polidea.rxandroidble;

import rx.Observable;

public interface RxBleDevice {

    Observable<Void> getState();

    Observable<RxBleConnection> getConnection();
}
