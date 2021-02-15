package com.polidea.rxandroidble3.internal.operations;

import android.bluetooth.BluetoothGatt;
import androidx.annotation.RequiresApi;

import com.polidea.rxandroidble3.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble3.exceptions.BleGattOperationType;
import com.polidea.rxandroidble3.internal.SingleResponseOperation;
import com.polidea.rxandroidble3.internal.connection.RxBleGattCallback;

import bleshadow.javax.inject.Inject;

import io.reactivex.rxjava3.core.Single;

public class ConnectionPriorityChangeOperation extends SingleResponseOperation<Long> {

    private final int connectionPriority;
    private final TimeoutConfiguration successTimeoutConfiguration;

    @Inject
    ConnectionPriorityChangeOperation(
            RxBleGattCallback rxBleGattCallback,
            BluetoothGatt bluetoothGatt,
            TimeoutConfiguration timeoutConfiguration,
            int connectionPriority,
            TimeoutConfiguration successTimeoutConfiguration) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.CONNECTION_PRIORITY_CHANGE, timeoutConfiguration);
        this.connectionPriority = connectionPriority;
        this.successTimeoutConfiguration = successTimeoutConfiguration;
    }

    @Override
    protected Single<Long> getCallback(RxBleGattCallback rxBleGattCallback) {
        return Single.timer(successTimeoutConfiguration.timeout, successTimeoutConfiguration.timeoutTimeUnit,
                successTimeoutConfiguration.timeoutScheduler);
    }

    @RequiresApi(21 /* Build.VERSION_CODES.LOLLIPOP */)
    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) throws IllegalArgumentException, BleGattCannotStartException {
        return bluetoothGatt.requestConnectionPriority(connectionPriority);
    }

    @Override
    public String toString() {
        return "ConnectionPriorityChangeOperation{"
                + super.toString()
                + ", connectionPriority=" + connectionPriorityToString(connectionPriority)
                + ", successTimeout=" + successTimeoutConfiguration
                + '}';
    }

    private static String connectionPriorityToString(int connectionPriority) {
        switch (connectionPriority) {
            case BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER:
                return "CONNECTION_PRIORITY_LOW_POWER";
            case BluetoothGatt.CONNECTION_PRIORITY_BALANCED:
                return "CONNECTION_PRIORITY_BALANCED";
            case BluetoothGatt.CONNECTION_PRIORITY_HIGH:
            default:
                return "CONNECTION_PRIORITY_HIGH";
        }
    }
}
