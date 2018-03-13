package com.polidea.rxandroidble2.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.polidea.rxandroidble2.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble2.exceptions.BleGattOperationType;
import com.polidea.rxandroidble2.internal.SingleResponseOperation;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;

import java.util.concurrent.TimeUnit;

import bleshadow.javax.inject.Inject;

import io.reactivex.Scheduler;
import io.reactivex.Single;

public class ConnectionPriorityChangeOperation extends SingleResponseOperation<Long> {

    private final int connectionPriority;
    private final long operationTimeout;
    private final TimeUnit timeUnit;
    private final Scheduler delayScheduler;

    @Inject
    ConnectionPriorityChangeOperation(
            RxBleGattCallback rxBleGattCallback,
            BluetoothGatt bluetoothGatt,
            TimeoutConfiguration timeoutConfiguration,
            int connectionPriority,
            long operationTimeout,
            TimeUnit timeUnit,
            Scheduler delayScheduler) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.CONNECTION_PRIORITY_CHANGE, timeoutConfiguration);
        this.connectionPriority = connectionPriority;
        this.operationTimeout = operationTimeout;
        this.timeUnit = timeUnit;
        this.delayScheduler = delayScheduler;
    }

    @Override
    protected Single<Long> getCallback(RxBleGattCallback rxBleGattCallback) {
        return Single.timer(operationTimeout, timeUnit, delayScheduler);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) throws IllegalArgumentException, BleGattCannotStartException {
        return bluetoothGatt.requestConnectionPriority(connectionPriority);
    }
}
