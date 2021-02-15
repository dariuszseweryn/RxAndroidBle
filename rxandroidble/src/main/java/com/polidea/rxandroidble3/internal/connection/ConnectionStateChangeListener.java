package com.polidea.rxandroidble3.internal.connection;


import com.polidea.rxandroidble3.RxBleConnection;

public interface ConnectionStateChangeListener {

    void onConnectionStateChange(RxBleConnection.RxBleConnectionState rxBleConnectionState);
}
