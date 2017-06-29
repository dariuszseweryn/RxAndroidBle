package com.polidea.rxandroidble.internal;

import com.jakewharton.rxrelay.BehaviorRelay;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.internal.connection.Connector;
import com.polidea.rxandroidble.internal.connection.ConnectorImpl;
import dagger.Binds;
import dagger.Module;
import rx.functions.Action1;

@Module
abstract class DeviceModuleBinder {

    @Binds
    abstract Connector bindConnector(ConnectorImpl rxBleConnectionConnector);

    @Binds
    abstract RxBleDevice bindDevice(RxBleDeviceImpl rxBleDevice);

    @Binds
    abstract Action1<RxBleConnection.RxBleConnectionState> bindConnectionStateChangedAction(
            BehaviorRelay<RxBleConnection.RxBleConnectionState> connectionStateBehaviorRelay
    );
}
