package com.polidea.rxandroidble.mockrxandroidble;

import android.content.Context;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;

import rx.Observable;

class RxBleDeviceMock implements RxBleDevice {

    private String name;
    private String macAddress;
    private RxBleConnection rxBleConnection;

    public RxBleDeviceMock(String name, String macAddress, RxBleConnection rxBleConnection) {
        this.name = name;
        this.macAddress = macAddress;
        this.rxBleConnection = rxBleConnection;
    }

    @Override
    public Observable<RxBleConnection> establishConnection(Context context) {
        return Observable.just(rxBleConnection);
    }

    @Override
    public Observable<RxBleConnection.RxBleConnectionState> getConnectionState() {
        throw new UnsupportedOperationException("Not implemented yet");
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
