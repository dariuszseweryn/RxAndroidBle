package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.polidea.rxandroidble.eventlog.OperationAttribute;
import com.polidea.rxandroidble.eventlog.OperationDescription;
import com.polidea.rxandroidble.eventlog.OperationEventLogger;
import com.polidea.rxandroidble.eventlog.OperationExtras;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.SingleResponseOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import bleshadow.javax.inject.Inject;
import rx.Observable;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MtuRequestOperation extends SingleResponseOperation<Integer> {

    private final int mtu;

    @Inject
    MtuRequestOperation(
            RxBleGattCallback rxBleGattCallback,
            BluetoothGatt bluetoothGatt,
            TimeoutConfiguration timeoutConfiguration, int requestedMtu, OperationEventLogger eventLogger) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.ON_MTU_CHANGED, timeoutConfiguration, eventLogger);
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

    @NonNull
    @Override
    protected OperationDescription createOperationDescription() {
        return new OperationDescription(new OperationAttribute(OperationExtras.REQUESTED_MTU, String.valueOf(mtu)));
    }

    @Nullable
    @Override
    protected String createOperationResultDescription(Integer result) {
        return "Set MTU: " + result;
    }
}
