package com.polidea.rxandroidble2.internal.operations;

import android.bluetooth.BluetoothGatt;

import com.polidea.rxandroidble2.exceptions.BleGattOperationType;
import com.polidea.rxandroidble2.internal.SingleResponseOperation;
import com.polidea.rxandroidble2.internal.connection.ConnectionModule;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

import io.reactivex.rxjava3.core.Single;

public class ReadRssiOperation extends SingleResponseOperation<Integer> {

    @Inject
    ReadRssiOperation(RxBleGattCallback bleGattCallback, BluetoothGatt bluetoothGatt,
                      @Named(ConnectionModule.OPERATION_TIMEOUT) TimeoutConfiguration timeoutConfiguration) {
        super(bluetoothGatt, bleGattCallback, BleGattOperationType.READ_RSSI, timeoutConfiguration);
    }

    @Override
    protected Single<Integer> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback.getOnRssiRead().firstOrError();
    }

    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) {
        return bluetoothGatt.readRemoteRssi();
    }

    @Override
    public String toString() {
        return "ReadRssiOperation{" + super.toString() + '}';
    }
}
