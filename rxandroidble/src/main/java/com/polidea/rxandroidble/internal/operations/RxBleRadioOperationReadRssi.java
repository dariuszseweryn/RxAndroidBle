package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import com.polidea.rxandroidble.exceptions.BleGattException;
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
        bleGattCallback
                .getOnRssiRead()
                .take(1)
                .doOnNext(integer -> releaseRadio())
                .subscribe(getSubscriber());

        final boolean success = bluetoothGatt.readRemoteRssi();
        if (!success) {
            onError(new BleGattException(-1, BleGattOperationType.READ_RSSI));
        }
    }
}
