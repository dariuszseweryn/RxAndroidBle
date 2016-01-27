package com.polidea.rxandroidble;

import android.content.Context;
import rx.Observable;

public interface RxBleDevice {

    Observable<RxBleConnection.RxBleConnectionState> getConnectionState();

    Observable<RxBleConnection> establishConnection(Context context);

    String getName();

    String getMacAddress();
}
