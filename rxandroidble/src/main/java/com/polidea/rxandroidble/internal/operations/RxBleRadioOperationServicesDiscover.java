package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleGattException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import java.util.concurrent.atomic.AtomicReference;

public class RxBleRadioOperationServicesDiscover extends RxBleRadioOperation<RxBleDeviceServices> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final AtomicReference<RxBleDeviceServices> rxBleDeviceServicesCache;

    public RxBleRadioOperationServicesDiscover(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt, AtomicReference<RxBleDeviceServices> rxBleDeviceServicesCache) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.rxBleDeviceServicesCache = rxBleDeviceServicesCache;
    }

    @Override
    public void run() {

        try {
            final RxBleDeviceServices rxBleDeviceServices = rxBleDeviceServicesCache.get();
            if (rxBleDeviceServices != null) {
                onNext(rxBleDeviceServices);
                onCompleted();
                return;
            }

            final boolean success = bluetoothGatt.discoverServices();
            if (!success) {
                onError(new BleGattException(BleGattOperationType.SERVICE_DISCOVERY));
                return;
            }

            final RxBleDeviceServices bleDeviceServices;
            try {
                bleDeviceServices = rxBleGattCallback.getOnServicesDiscovered().toBlocking().first();
            } catch (Exception e) {
                onError(e);
                return;
            }

            rxBleDeviceServicesCache.set(bleDeviceServices);
            onNext(bleDeviceServices);
            onCompleted();

        } finally {
            releaseRadio();
        }
    }
}
