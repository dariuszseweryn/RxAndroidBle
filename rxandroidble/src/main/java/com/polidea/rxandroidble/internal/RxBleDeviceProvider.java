package com.polidea.rxandroidble.internal;

import android.bluetooth.BluetoothDevice;

import com.polidea.rxandroidble.RxBleAdapterStateObservable;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.internal.cache.RxBleDeviceCache;
import com.polidea.rxandroidble.internal.connection.RxBleConnectionConnectorImpl;
import com.polidea.rxandroidble.internal.connection.RxBleConnectionConnectorOperationsProvider;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.util.BleConnectionCompat;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;

import java.util.Map;
import rx.Observable;

public class RxBleDeviceProvider {

    private final Map<String, RxBleDevice> availableDevices = new RxBleDeviceCache();
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final RxBleRadio rxBleRadio;
    private final BleConnectionCompat bleConnectionCompat;
    private final Observable<RxBleAdapterStateObservable.BleAdapterState> adapterStateObservable;

    public RxBleDeviceProvider(RxBleAdapterWrapper rxBleAdapterWrapper, RxBleRadio rxBleRadio, BleConnectionCompat bleConnectionCompat,
                               Observable<RxBleAdapterStateObservable.BleAdapterState> adapterStateObservable) {
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.rxBleRadio = rxBleRadio;
        this.bleConnectionCompat = bleConnectionCompat;
        this.adapterStateObservable = adapterStateObservable;
    }

    public RxBleDevice getBleDevice(String macAddress) {
        final RxBleDevice rxBleDevice = availableDevices.get(macAddress);

        if (rxBleDevice != null) {
            return rxBleDevice;
        }

        synchronized (availableDevices) {
            final RxBleDevice secondCheckRxBleDevice = availableDevices.get(macAddress);

            if (secondCheckRxBleDevice != null) {
                return secondCheckRxBleDevice;
            }

            final BluetoothDevice bluetoothDevice = rxBleAdapterWrapper.getRemoteDevice(macAddress);
            final RxBleDeviceImpl newRxBleDevice = new RxBleDeviceImpl(
                    bluetoothDevice,
                    new RxBleConnectionConnectorImpl(bluetoothDevice,
                            RxBleGattCallback::new,
                            new RxBleConnectionConnectorOperationsProvider(),
                            rxBleRadio,
                            bleConnectionCompat,
                            rxBleAdapterWrapper,
                            adapterStateObservable)
            );
            availableDevices.put(macAddress, newRxBleDevice);
            return newRxBleDevice;
        }
    }
}
