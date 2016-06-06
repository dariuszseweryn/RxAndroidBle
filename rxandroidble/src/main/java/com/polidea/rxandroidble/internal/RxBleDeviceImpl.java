package com.polidea.rxandroidble.internal;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.exceptions.BleAlreadyConnectedException;

import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.subjects.BehaviorSubject;

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTED;
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTING;
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.DISCONNECTED;

class RxBleDeviceImpl implements RxBleDevice {

    private final BluetoothDevice bluetoothDevice;
    private final RxBleConnection.Connector connector;
    private final BehaviorSubject<RxBleConnection.RxBleConnectionState> connectionStateSubject = BehaviorSubject.create(DISCONNECTED);
    private AtomicBoolean isConnected = new AtomicBoolean(false);

    public RxBleDeviceImpl(BluetoothDevice bluetoothDevice, RxBleConnection.Connector connector) {
        this.bluetoothDevice = bluetoothDevice;
        this.connector = connector;
    }

    @Override
    public Observable<RxBleConnection.RxBleConnectionState> observeConnectionStateChanges() {
        return connectionStateSubject.distinctUntilChanged();
    }

    @Override
    public RxBleConnection.RxBleConnectionState getConnectionState() {
        return observeConnectionStateChanges().toBlocking().first();
    }

    @Override
    public Observable<RxBleConnection> establishConnection(Context context, boolean autoConnect) {
        return Observable.defer(() -> {

                if (isConnected.compareAndSet(false, true)) {
                    return connector.prepareConnection(context, autoConnect)
                            .doOnSubscribe(() -> connectionStateSubject.onNext(CONNECTING))
                            .doOnNext(rxBleConnection -> connectionStateSubject.onNext(CONNECTED))
                            .doOnUnsubscribe(() -> {
                                connectionStateSubject.onNext(DISCONNECTED);
                                isConnected.set(false);
                            });
                } else {
                    return Observable.error(new BleAlreadyConnectedException(bluetoothDevice.getAddress()));
                }
        });
    }

    @Override
    public String getName() {
        return bluetoothDevice.getName();
    }

    @Override
    public String getMacAddress() {
        return bluetoothDevice.getAddress();
    }

    @Override
    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RxBleDeviceImpl)) {
            return false;
        }

        RxBleDeviceImpl that = (RxBleDeviceImpl) o;
        return bluetoothDevice.equals(that.bluetoothDevice);
    }

    @Override
    public int hashCode() {
        return bluetoothDevice.hashCode();
    }

    @Override
    public String toString() {
        return "RxBleDeviceImpl{" + "bluetoothDevice=" + bluetoothDevice.getName() + '(' + bluetoothDevice.getAddress() + ')' + '}';
    }
}
