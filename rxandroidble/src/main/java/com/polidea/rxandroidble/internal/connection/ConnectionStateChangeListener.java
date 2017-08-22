package com.polidea.rxandroidble.internal.connection;


import com.polidea.rxandroidble.RxBleConnection;

public interface ConnectionStateChangeListener {

    void onConnectionStateChange(RxBleConnection.RxBleConnectionState rxBleConnectionState);
}
