package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import rx.Subscription;

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
        final Subscription subscription = bleGattCallback
                .getOnRssiRead()
                .take(1)
                .doOnCompleted(() -> releaseRadio())
                .subscribe(getSubscriber());

        final boolean success = bluetoothGatt.readRemoteRssi();
        if (!success) {
            subscription.unsubscribe();
            onError(new BleGattCannotStartException(BleGattOperationType.READ_RSSI));
        }
    }
}
