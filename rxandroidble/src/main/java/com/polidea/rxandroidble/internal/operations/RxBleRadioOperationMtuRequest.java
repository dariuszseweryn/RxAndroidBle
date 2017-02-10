package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.os.Build;
import android.support.annotation.RequiresApi;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleGattRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Scheduler;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RxBleRadioOperationMtuRequest extends RxBleGattRadioOperation<Integer> {

    private final int mtu;

    public RxBleRadioOperationMtuRequest(
            int mtu,
            RxBleGattCallback rxBleGattCallback,
            BluetoothGatt bluetoothGatt,
            long timeout,
            TimeUnit timeoutTimeUnit,
            Scheduler timeoutScheduler) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.ON_MTU_CHANGED, timeout, timeoutTimeUnit, timeoutScheduler);
        this.mtu = mtu;
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
