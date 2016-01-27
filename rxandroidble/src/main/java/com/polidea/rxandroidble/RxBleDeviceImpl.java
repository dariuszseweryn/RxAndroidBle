package com.polidea.rxandroidble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import com.polidea.rxandroidble.internal.RxBleRadio;
import rx.Observable;

public class RxBleDeviceImpl implements RxBleDevice {

    private final BluetoothDevice bluetoothDevice;

    private final RxBleRadio rxBleRadio;

    public RxBleDeviceImpl(BluetoothDevice bluetoothDevice, RxBleRadio rxBleRadio) {
        this.bluetoothDevice = bluetoothDevice;
        this.rxBleRadio = rxBleRadio;
    }

    public Observable<RxBleConnection.RxBleConnectionState> getConnectionState() {
        return null;
    }

    public Observable<RxBleConnection> establishConnection(Context context) {
        final RxBleConnectionImpl rxBleConnection = new RxBleConnectionImpl(bluetoothDevice, rxBleRadio); // TODO: add managing connections
        return rxBleConnection.connect(context);
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
