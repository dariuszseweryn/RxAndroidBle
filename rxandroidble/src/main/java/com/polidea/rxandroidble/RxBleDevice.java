package com.polidea.rxandroidble;

import rx.Observable;

public interface RxBleDevice {

    Observable<RxBleConnection.RxBleConnectionState> getState();

    Observable<RxBleConnection> establishConnection();
}
