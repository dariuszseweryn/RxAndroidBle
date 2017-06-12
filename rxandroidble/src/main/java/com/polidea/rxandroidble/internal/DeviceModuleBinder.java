package com.polidea.rxandroidble.internal;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.internal.connection.RxBleConnectionConnectorImpl;
import dagger.Binds;
import dagger.Module;

@Module
abstract public class DeviceModuleBinder {

    @Binds
    abstract RxBleConnection.Connector bindConnector(RxBleConnectionConnectorImpl rxBleConnectionConnector);

    @Binds
    abstract RxBleDevice bindDevice(RxBleDeviceImpl rxBleDevice);
}
