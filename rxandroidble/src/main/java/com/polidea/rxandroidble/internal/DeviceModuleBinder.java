package com.polidea.rxandroidble.internal;

import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.internal.connection.Connector;
import com.polidea.rxandroidble.internal.connection.ConnectorImpl;
import bleshadow.dagger.Binds;
import bleshadow.dagger.Module;

@Module
abstract class DeviceModuleBinder {

    @Binds
    abstract Connector bindConnector(ConnectorImpl rxBleConnectionConnector);

    @Binds
    abstract RxBleDevice bindDevice(RxBleDeviceImpl rxBleDevice);
}
