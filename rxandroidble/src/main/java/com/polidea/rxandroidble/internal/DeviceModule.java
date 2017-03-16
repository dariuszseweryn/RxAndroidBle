package com.polidea.rxandroidble.internal;

import android.bluetooth.BluetoothDevice;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.ClientComponent.NamedSchedulers;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.internal.connection.ConnectionComponent;
import com.polidea.rxandroidble.internal.connection.RxBleConnectionConnectorImpl;
import com.polidea.rxandroidble.internal.operations.TimeoutConfiguration;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;

import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import rx.Scheduler;

@Module(subcomponents = ConnectionComponent.class)
public class DeviceModule {

    public static final String MAC_ADDRESS = "mac-address";
    public static final String OPERATION_TIMEOUT = "operation-timeout";
    public static final String DISCONNECT_TIMEOUT = "disconnect-timeout";
    public static final String CONNECT_TIMEOUT = "connect-timeout";

    private static final int DEFAULT_OPERATION_TIMEOUT = 30;
    private static final int DEFAULT_DISCONNECT_TIMEOUT = 10;
    private static final int DEFAULT_CONNECT_TIMEOUT = 35;
    final String macAddress;

    DeviceModule(String macAddress) {
        this.macAddress = macAddress;
    }

    @Provides
    BluetoothDevice provideBluetoothDevice(RxBleAdapterWrapper adapterWrapper) {
        return adapterWrapper.getRemoteDevice(macAddress);
    }

    @Provides
    @Named(MAC_ADDRESS)
    String provideMacAddress() {
        return macAddress;
    }

    @Provides
    RxBleConnection.Connector provideRxBleConnectionConnector(RxBleConnectionConnectorImpl rxBleConnectionConnector) {
        return rxBleConnectionConnector;
    }

    @Provides
    RxBleDevice provideRxBleDevice(RxBleDeviceImpl rxBleDevice) {
        return rxBleDevice;
    }

    @Provides
    @Named(OPERATION_TIMEOUT)
    TimeoutConfiguration providesOperationTimeoutConfiguration(@Named(NamedSchedulers.TIMEOUT) Scheduler timeoutScheduler) {
        return new TimeoutConfiguration(DEFAULT_OPERATION_TIMEOUT, TimeUnit.SECONDS, timeoutScheduler);
    }

    @Provides
    @Named(CONNECT_TIMEOUT)
    TimeoutConfiguration providesConnectTimeoutConfiguration(@Named(ClientComponent.NamedSchedulers.TIMEOUT) Scheduler timeoutScheduler) {
        return new TimeoutConfiguration(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS, timeoutScheduler);
    }

    @Provides
    @Named(DISCONNECT_TIMEOUT)
    TimeoutConfiguration providesDisconnectTimeoutConfiguration(@Named(NamedSchedulers.TIMEOUT) Scheduler timeoutScheduler) {
        return new TimeoutConfiguration(DEFAULT_DISCONNECT_TIMEOUT, TimeUnit.SECONDS, timeoutScheduler);
    }
}
