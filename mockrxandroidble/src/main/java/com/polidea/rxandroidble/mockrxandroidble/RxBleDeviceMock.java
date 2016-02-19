package com.polidea.rxandroidble.mockrxandroidble;

import android.content.Context;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.internal.RxBleConnectibleConnection;

import rx.Observable;
import rx.subjects.Subject;

class RxBleDeviceMock implements RxBleDevice {

    private String name;
    private String macAddress;
    private RxBleConnectibleConnection rxBleConnection;
    private Subject<RxBleConnection.RxBleConnectionState, RxBleConnection.RxBleConnectionState> connectionStateSubject;

    public RxBleDeviceMock(String name, String macAddress, Subject<RxBleConnection.RxBleConnectionState, RxBleConnection.RxBleConnectionState> connectionStateSubject, RxBleConnectibleConnection rxBleConnection) {
        this.name = name;
        this.macAddress = macAddress;
        this.rxBleConnection = rxBleConnection;
        this.connectionStateSubject = connectionStateSubject;
    }

    @Override
    public Observable<RxBleConnection> establishConnection(Context context, boolean autoConnect) {
        return rxBleConnection.connect(context, autoConnect)
                .doOnError(t -> connectionStateSubject.onNext(RxBleConnection.RxBleConnectionState.DISCONNECTED))
                .doOnUnsubscribe(() -> connectionStateSubject.onNext(RxBleConnection.RxBleConnectionState.DISCONNECTED));
    }

    @Override
    public Observable<RxBleConnection.RxBleConnectionState> getConnectionState() {
        return connectionStateSubject;
    }

    @Override
    public String getMacAddress() {
        return macAddress;
    }

    @Override
    public String getName() {
        return name;
    }
}
