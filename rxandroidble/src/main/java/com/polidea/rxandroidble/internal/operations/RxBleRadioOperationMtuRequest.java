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
import rx.Subscription;
import rx.functions.Action0;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class RxBleRadioOperationMtuRequest extends RxBleGattRadioOperation<Integer> {

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
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.ON_MTU_CHANGED);
        this.mtu = mtu;
        this.timeout = timeout;
        this.timeoutTimeUnit = timeoutTimeUnit;
        this.timeoutScheduler = timeoutScheduler;
    }

    @Override
    protected void protectedRun() {
        final Subscription subscription = rxBleGattCallback
                .getOnMtuChanged()
                .first()
                .timeout(
                        timeout,
                        timeoutTimeUnit,
                        Observable.<Integer>error(newTimeoutException()),
                        timeoutScheduler
                )
                .doOnTerminate(new Action0() {
                    @Override
                    public void call() {
                        RxBleRadioOperationMtuRequest.this.releaseRadio();
                    }
                })
                .subscribe(getSubscriber());

        boolean success = bluetoothGatt.requestMtu(mtu);
        if (!success) {
            subscription.unsubscribe();
            onError(newCannotStartException());
        }
    }
}
