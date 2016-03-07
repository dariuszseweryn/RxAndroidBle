package com.polidea.rxandroidble.mockrxandroidble;

import android.content.Context;

import com.polidea.rxandroidble.RxBleConnection;

import rx.Observable;

public class RxBleConnectionConnectorMock implements RxBleConnection.Connector {

    private final RxBleConnection rxBleConnection;

    public RxBleConnectionConnectorMock(RxBleConnection rxBleConnection) {
        this.rxBleConnection = rxBleConnection;
    }

    @Override
    public Observable<RxBleConnection> prepareConnection(Context context, boolean autoConnect) {
        return Observable.defer(() -> Observable.just(rxBleConnection));
    }

}
