package com.polidea.rxandroidble;

import android.content.Context;

import rx.Observable;

public interface RxBleDevice {

    /**
     * This observable returns only actual state of the BLE connection - it doesn't transmit errors.
     * On subscription returns immediately last known RxBleConnectionState.
     *
     * @return the most current RxBleConnectionState
     */
    Observable<RxBleConnection.RxBleConnectionState> getConnectionState();

    // TODO: [PU] 15.03.2016 Document how multiple connections are handled, when connection is connected and disconnected.
    Observable<RxBleConnection> establishConnection(Context context, boolean autoConnect);

    String getName();

    String getMacAddress();
}
