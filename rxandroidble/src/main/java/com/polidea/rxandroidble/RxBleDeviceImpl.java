package com.polidea.rxandroidble;

import android.bluetooth.BluetoothDevice;
import rx.Observable;

public class RxBleDeviceImpl implements RxBleDevice {

    private final BluetoothDevice bluetoothDevice;

    public RxBleDeviceImpl(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public Observable<Void> getState() {
        return null;
    }

    public Observable<RxBleConnection> getConnection() {
        return Observable.create(subscriber -> new RxBleConnectionImpl(bluetoothDevice));
    }
}
