package com.polidea.rxandroidble2.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.util.Pair;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import com.polidea.rxandroidble2.exceptions.BleGattOperationType;
import com.polidea.rxandroidble2.internal.SingleResponseOperation;
import com.polidea.rxandroidble2.internal.connection.ConnectionModule;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import io.reactivex.Single;

@RequiresApi(26 /* Build.VERSION_CODES.O */)
public class PhyReadOperation extends SingleResponseOperation<Pair<Integer, Integer>> {

    @Inject
    PhyReadOperation(RxBleGattCallback bleGattCallback, BluetoothGatt bluetoothGatt,
                     @Named(ConnectionModule.OPERATION_TIMEOUT) TimeoutConfiguration timeoutConfiguration) {
        super(bluetoothGatt, bleGattCallback, BleGattOperationType.PHY_READ, timeoutConfiguration);
    }

    @Override
    protected Single<Pair<Integer, Integer>> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback.getOnPhyRead().firstOrError();
    }

    @Override
    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    protected boolean startOperation(BluetoothGatt bluetoothGatt) {
        bluetoothGatt.readPhy();
        return true;
    }

    @Override
    public String toString() {
        return "PhyReadOperation{" + super.toString() + '}';
    }
}
