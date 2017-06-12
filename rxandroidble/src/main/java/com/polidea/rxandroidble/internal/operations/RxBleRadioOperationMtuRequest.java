package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleSingleGattRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import javax.inject.Inject;

import rx.Observable;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RxBleRadioOperationMtuRequest extends RxBleSingleGattRadioOperation<Integer> {

    private final int mtu;

    @Inject
    RxBleRadioOperationMtuRequest(
            RxBleGattCallback rxBleGattCallback,
            BluetoothGatt bluetoothGatt,
            TimeoutConfiguration timeoutConfiguration, int requestedMtu) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.ON_MTU_CHANGED, timeoutConfiguration);
        mtu = requestedMtu;
    }

    @Override
    protected Observable<Integer> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback.getOnMtuChanged();
    }

    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) {
        return bluetoothGatt.requestMtu(mtu);
    }
}
