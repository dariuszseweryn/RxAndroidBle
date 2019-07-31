package com.polidea.rxandroidble2.internal.connection;


import com.polidea.rxandroidble2.ConnectionSetup;
import com.polidea.rxandroidble2.RxBleConnection;

import io.reactivex.rxjava3.core.Observable;

public interface Connector {

    Observable<RxBleConnection> prepareConnection(ConnectionSetup autoConnect);
}
