package com.polidea.rxandroidble3.internal.connection;


import com.polidea.rxandroidble3.ConnectionSetup;
import com.polidea.rxandroidble3.RxBleConnection;

import io.reactivex.rxjava3.core.Observable;

public interface Connector {

    Observable<RxBleConnection> prepareConnection(ConnectionSetup autoConnect);
}
