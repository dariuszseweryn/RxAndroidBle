package com.polidea.rxandroidble.internal;

import android.content.Context;

import com.polidea.rxandroidble.RxBleConnection;

import rx.Observable;

public interface RxBleConnectibleConnection extends RxBleConnection {

    Observable<RxBleConnection> connect(Context context);
}
