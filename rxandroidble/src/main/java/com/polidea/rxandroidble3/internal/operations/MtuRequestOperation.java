package com.polidea.rxandroidble2.internal.operations;

import android.bluetooth.BluetoothGatt;
import androidx.annotation.RequiresApi;

import com.polidea.rxandroidble2.exceptions.BleGattOperationType;
import com.polidea.rxandroidble2.internal.SingleResponseOperation;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;

import bleshadow.javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

@RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
public class MtuRequestOperation extends SingleResponseOperation<Integer> {

    private final int mtu;

    @Inject
    MtuRequestOperation(
            RxBleGattCallback rxBleGattCallback,
            BluetoothGatt bluetoothGatt,
            TimeoutConfiguration timeoutConfiguration, int requestedMtu) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.ON_MTU_CHANGED, timeoutConfiguration);
        mtu = requestedMtu;
    }

    @Override
    protected Single<Integer> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback.getOnMtuChanged().firstOrError();
    }

    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) {
        return bluetoothGatt.requestMtu(mtu);
    }

    @Override
    public String toString() {
        return "MtuRequestOperation{"
                + super.toString()
                + ", mtu=" + mtu
                + '}';
    }
}
