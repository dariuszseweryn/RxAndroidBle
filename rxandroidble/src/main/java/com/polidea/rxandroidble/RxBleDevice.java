package com.polidea.rxandroidble;

import rx.Observable;

public interface RxBleDevice {

    Observable<RxBleConnection.RxBleConnectionState> getConnectionState();

    Observable<RxBleConnection> establishConnection();
}
