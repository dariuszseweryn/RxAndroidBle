package com.polidea.rxandroidble.internal;

import android.bluetooth.BluetoothDevice;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleDeviceImpl;

import com.polidea.rxandroidble.internal.connection.RxBleConnectionConnectorImpl;
import com.polidea.rxandroidble.internal.connection.RxBleConnectionConnectorOperationsProvider;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import java.util.Map;

public class RxBleDeviceProvider {

    private final Map<String, RxBleDevice> availableDevices = new RxBleDeviceCache();
    private final RxBleAdapterWrapper rxBleAdapterWrapper;
    private final RxBleRadio rxBleRadio;

    public RxBleDeviceProvider(RxBleAdapterWrapper rxBleAdapterWrapper, RxBleRadio rxBleRadio) {
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.rxBleRadio = rxBleRadio;
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
                    new RxBleConnectionConnectorImpl(bluetoothDevice, RxBleGattCallback::new, new RxBleConnectionConnectorOperationsProvider(), rxBleRadio)
            );
            availableDevices.put(macAddress, newRxBleDevice);
            return newRxBleDevice;
        }
    }
}
