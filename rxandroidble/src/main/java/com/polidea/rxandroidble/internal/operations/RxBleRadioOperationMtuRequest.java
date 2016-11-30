package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RxBleRadioOperationMtuRequest extends RxBleRadioOperation<Integer> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final int mtu;

    public RxBleRadioOperationMtuRequest(
            int mtu,
            RxBleGattCallback rxBleGattCallback,
            BluetoothGatt bluetoothGatt) {
        this.mtu = mtu;
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
    }

    @Override
    protected void protectedRun() {
        final Subscription subscription = rxBleGattCallback
                .getOnMtuChanged()
                .first()
                .doOnTerminate(() -> releaseRadio())
                .subscribe(getSubscriber());

        boolean success = bluetoothGatt.requestMtu(mtu);
        if (!success) {
            subscription.unsubscribe();
            onError(new BleGattCannotStartException(BleGattOperationType.ON_MTU_CHANGED));
        }
    }
}
