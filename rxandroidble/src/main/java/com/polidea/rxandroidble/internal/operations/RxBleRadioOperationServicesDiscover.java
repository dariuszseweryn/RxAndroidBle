package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;

import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleGattException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;

public class RxBleRadioOperationServicesDiscover extends RxBleRadioOperation<RxBleDeviceServices> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    public RxBleRadioOperationServicesDiscover(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
    }

    @Override
    public void run() {
        //noinspection Convert2MethodRef
        rxBleGattCallback
                .getOnServicesDiscovered()
                .take(1)
                .doOnCompleted(() -> releaseRadio())
                .subscribe(getSubscriber());

        final boolean success = bluetoothGatt.discoverServices();
        if (!success) {
            onError(new BleGattException(BleGattOperationType.SERVICE_DISCOVERY));
        }
    }

    @Override
    protected Priority definedPriority() {
        return Priority.HIGH;
    }
}
