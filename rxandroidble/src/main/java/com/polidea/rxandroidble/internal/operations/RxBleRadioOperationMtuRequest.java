package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;

public class RxBleRadioOperationMtuRequest extends RxBleRadioOperation<Integer> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final long timeout;

    private final TimeUnit timeoutTimeUnit;

    private final Scheduler timeoutScheduler;

    private final int mtu;

    public RxBleRadioOperationMtuRequest(
            int mtu,
            RxBleGattCallback rxBleGattCallback,
            BluetoothGatt bluetoothGatt,
            long timeout,
            TimeUnit timeoutTimeUnit,
            Scheduler timeoutScheduler) {
        this.mtu = mtu;
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.timeout = timeout;
        this.timeoutTimeUnit = timeoutTimeUnit;
        this.timeoutScheduler = timeoutScheduler;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void protectedRun() {
        final Subscription subscription = rxBleGattCallback
                .getOnMtuChanged()
                .first()
                .timeout(timeout, timeoutTimeUnit, timeoutFallbackProcedure(), timeoutScheduler)
                .doOnTerminate(() -> releaseRadio())
                .subscribe(getSubscriber());

        boolean success = bluetoothGatt.requestMtu(mtu);
        if (!success) {
            subscription.unsubscribe();
            onError(new BleGattCannotStartException(BleGattOperationType.ON_MTU_CHANGED));
        }
    }

    @NonNull
    private Observable<Integer> timeoutFallbackProcedure() {
        return Observable.error(new TimeoutException());
    }
}
