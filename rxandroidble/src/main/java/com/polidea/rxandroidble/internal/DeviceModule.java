package com.polidea.rxandroidble.internal;

import android.bluetooth.BluetoothDevice;

import com.jakewharton.rxrelay.BehaviorRelay;
import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.ClientComponent.NamedSchedulers;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.connection.ConnectionComponent;
import com.polidea.rxandroidble.internal.connection.ConnectionStateChangeListener;
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
    @Named(OPERATION_TIMEOUT)
    static TimeoutConfiguration providesOperationTimeoutConf(@Named(NamedSchedulers.TIMEOUT) Scheduler timeoutScheduler) {
        return new TimeoutConfiguration(DEFAULT_OPERATION_TIMEOUT, TimeUnit.SECONDS, timeoutScheduler);
    }

    @Provides
    @Named(CONNECT_TIMEOUT)
    static TimeoutConfiguration providesConnectTimeoutConf(@Named(ClientComponent.NamedSchedulers.TIMEOUT) Scheduler timeoutScheduler) {
        return new TimeoutConfiguration(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS, timeoutScheduler);
    }

    @Provides
    @Named(DISCONNECT_TIMEOUT)
    static TimeoutConfiguration providesDisconnectTimeoutConf(@Named(NamedSchedulers.TIMEOUT) Scheduler timeoutScheduler) {
        return new TimeoutConfiguration(DEFAULT_DISCONNECT_TIMEOUT, TimeUnit.SECONDS, timeoutScheduler);
    }

    @Provides
    @DeviceScope
    static BehaviorRelay<RxBleConnection.RxBleConnectionState> provideConnectionStateRelay() {
        return BehaviorRelay.create(RxBleConnection.RxBleConnectionState.DISCONNECTED);
    }

    @Provides
    @DeviceScope
    static ConnectionStateChangeListener provideConnectionStateChangeListener(
            final BehaviorRelay<RxBleConnection.RxBleConnectionState> connectionStateBehaviorRelay
    ) {
        return new ConnectionStateChangeListener() {
            @Override
            public void onConnectionStateChange(RxBleConnection.RxBleConnectionState rxBleConnectionState) {
                connectionStateBehaviorRelay.call(rxBleConnectionState);
            }
        };
    }
}
