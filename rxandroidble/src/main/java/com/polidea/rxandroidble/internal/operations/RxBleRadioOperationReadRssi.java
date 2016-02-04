package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;

public class RxBleRadioOperationReadRssi extends RxBleRadioOperation<Integer> {

    private final RxBleGattCallback bleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    public RxBleRadioOperationReadRssi(RxBleGattCallback bleGattCallback, BluetoothGatt bluetoothGatt) {
        this.bleGattCallback = bleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
    }

    @Override
    public void run() {
        //noinspection Convert2MethodRef
        bleGattCallback
                .getOnRssiRead()
                .take(1)
                .doOnCompleted(() -> releaseRadio())
                .subscribe(getSubscriber());
        // TODO: [PU] 29.01.2016 Release radio on error as well?
        final boolean success = bluetoothGatt.readRemoteRssi();
        if (!success) {
            onError(new BleGattCannotStartException(BleGattOperationType.READ_RSSI));
        }
    }
}
