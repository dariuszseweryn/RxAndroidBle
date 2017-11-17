package com.polidea.rxandroidble.internal.connection;


import com.polidea.rxandroidble.ConnectionSetup;
import com.polidea.rxandroidble.RxBleConnection;
import rx.Observable;

public interface Connector {

    Observable<RxBleConnection> prepareConnection(ConnectionSetup autoConnect);
}
