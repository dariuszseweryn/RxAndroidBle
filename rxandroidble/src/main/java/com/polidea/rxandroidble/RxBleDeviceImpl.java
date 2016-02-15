package com.polidea.rxandroidble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import com.polidea.rxandroidble.internal.RxBleRadio;
import java.util.concurrent.atomic.AtomicReference;
import rx.Observable;

public class RxBleDeviceImpl implements RxBleDevice {

    private final BluetoothDevice bluetoothDevice;

    private final RxBleRadio rxBleRadio;

    private final AtomicReference<Observable<RxBleConnection>> connectionObservable = new AtomicReference<>();

    public RxBleDeviceImpl(BluetoothDevice bluetoothDevice, RxBleRadio rxBleRadio) {
        this.bluetoothDevice = bluetoothDevice;
        this.rxBleRadio = rxBleRadio;
    }

    public Observable<RxBleConnection.RxBleConnectionState> getConnectionState() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Observable<RxBleConnection> establishConnection(Context context) {
        synchronized (connectionObservable) {
            final Observable<RxBleConnection> rxBleConnectionObservable = connectionObservable.get();
            if (rxBleConnectionObservable != null) {
                return rxBleConnectionObservable;
            }

            final Observable<RxBleConnection> newConnectionObservable =
                    new RxBleConnectionImpl(bluetoothDevice, rxBleRadio)
                            .connect(context)
                            .replay()
                            .refCount()
                            .doOnUnsubscribe(() -> connectionObservable.set(null)); //FIXME: [DS] 11.02.2016 Potential race condition when one subscriber would like to just after the previous one has unsubscribed
            connectionObservable.set(newConnectionObservable);
            return newConnectionObservable;
        }
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
    public String toString() {
        return "RxBleDeviceImpl{" +
                "bluetoothDevice=" + bluetoothDevice.getName() + '(' + bluetoothDevice.getAddress() + ')' +
                '}';
    }
}
