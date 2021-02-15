package com.polidea.rxandroidble2.internal.connection;


import com.polidea.rxandroidble2.RxBleConnection;

public interface ConnectionStateChangeListener {

    void onConnectionStateChange(RxBleConnection.RxBleConnectionState rxBleConnectionState);
}
