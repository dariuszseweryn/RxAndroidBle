package com.polidea.rxandroidble;

import android.content.Context;

import rx.Observable;

public interface RxBleDevice {

    /**
     * This observable returns only actual state of the BLE connection - it doesn't transmit errors.
     * On subscription returns immediately last known RxBleConnectionState.
     * @return the most current RxBleConnectionState
     */
    Observable<RxBleConnection.RxBleConnectionState> getConnectionState();

    Observable<RxBleConnection> establishConnection(Context context, boolean autoConnect);

    String getName();

    String getMacAddress();
}
