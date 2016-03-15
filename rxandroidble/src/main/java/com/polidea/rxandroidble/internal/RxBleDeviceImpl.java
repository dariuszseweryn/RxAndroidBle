package com.polidea.rxandroidble.internal;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.subjects.BehaviorSubject;

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTED;
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTING;
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.DISCONNECTED;

class RxBleDeviceImpl implements RxBleDevice {

    private final BluetoothDevice bluetoothDevice;
    private final RxBleConnection.Connector connector;
    private final AtomicReference<Observable<RxBleConnection>> connectionObservable = new AtomicReference<>();
    private final BehaviorSubject<RxBleConnection.RxBleConnectionState> connectionStateSubject = BehaviorSubject.create(DISCONNECTED);

    public RxBleDeviceImpl(BluetoothDevice bluetoothDevice, RxBleConnection.Connector connector) {
        this.bluetoothDevice = bluetoothDevice;
        this.connector = connector;
    }

    public Observable<RxBleConnection.RxBleConnectionState> getConnectionState() {
        return connectionStateSubject.distinctUntilChanged();
    }

    public Observable<RxBleConnection> establishConnection(Context context, boolean autoConnect) {
        return Observable.defer(() -> {

            synchronized (connectionObservable) {
                final Observable<RxBleConnection> rxBleConnectionObservable = connectionObservable.get();
                if (rxBleConnectionObservable != null) {
                    return rxBleConnectionObservable;
                }

                final Observable<RxBleConnection> newConnectionObservable =
                        connector.prepareConnection(context, autoConnect)
                                .doOnSubscribe(() -> connectionStateSubject.onNext(CONNECTING))
                                .doOnNext(rxBleConnection -> connectionStateSubject.onNext(CONNECTED))
                                .doOnUnsubscribe(() -> {
                                    synchronized (connectionObservable) {
                                        //FIXME: [DS] 11.02.2016 Potential race condition when one subscriber would like to just after the previous one has unsubscribed
                                        connectionObservable.set(null);
                                    }
                                    connectionStateSubject.onNext(DISCONNECTED);
                                })
                                .replay()
                                .refCount();

                connectionObservable.set(newConnectionObservable);
                return newConnectionObservable;
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
        return "RxBleDeviceImpl{" +
                "bluetoothDevice=" + bluetoothDevice.getName() + '(' + bluetoothDevice.getAddress() + ')' +
                '}';
    }
}
