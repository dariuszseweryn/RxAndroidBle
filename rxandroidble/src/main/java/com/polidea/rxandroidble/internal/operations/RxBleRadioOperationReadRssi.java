package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleGattRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Scheduler;

public class RxBleRadioOperationReadRssi extends RxBleGattRadioOperation<Integer> {

    public RxBleRadioOperationReadRssi(RxBleGattCallback bleGattCallback, BluetoothGatt bluetoothGatt, Scheduler timeoutScheduler) {
        super(bluetoothGatt, bleGattCallback, BleGattOperationType.READ_RSSI, 30, TimeUnit.SECONDS, timeoutScheduler);
    }

    @Override
    protected Observable<Integer> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback.getOnRssiRead();
    }

    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) {
        return bluetoothGatt.readRemoteRssi();
    }
}
