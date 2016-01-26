package com.polidea.rxandroidble;

import android.bluetooth.BluetoothAdapter;
import android.support.annotation.Nullable;
import java.util.UUID;
import rx.Observable;

public class RxBleClientImpl implements RxBleClient {

    @Override
    public Observable<RxBleScanResult> scanBleDevices(@Nullable UUID[] filterServiceUUIDs) {
        final BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        defaultAdapter.startLeScan((device, rssi, scanRecord) -> {

        });
        return null;
    }

    public RxBleDeviceImpl getBleDevice(String bluetoothAddress) {
        return null;
    }
}
