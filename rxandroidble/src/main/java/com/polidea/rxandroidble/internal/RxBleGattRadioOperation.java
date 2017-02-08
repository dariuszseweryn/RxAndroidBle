package com.polidea.rxandroidble.internal;


import android.bluetooth.BluetoothGatt;
import android.os.DeadObjectException;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

public abstract class RxBleGattRadioOperation<T> extends RxBleRadioOperation<T> {

    protected final BluetoothGatt bluetoothGatt;

    protected final RxBleGattCallback rxBleGattCallback;

    protected final BleGattOperationType operationType;

    public RxBleGattRadioOperation(BluetoothGatt bluetoothGatt, RxBleGattCallback rxBleGattCallback, BleGattOperationType operationType) {
        this.bluetoothGatt = bluetoothGatt;
        this.rxBleGattCallback = rxBleGattCallback;
        this.operationType = operationType;
    }

    protected BleGattCallbackTimeoutException newTimeoutException() {
        return new BleGattCallbackTimeoutException(bluetoothGatt, operationType);
    }

    protected BleGattCannotStartException newCannotStartException() {
        return new BleGattCannotStartException(bluetoothGatt, operationType);
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleDisconnectedException(deadObjectException, bluetoothGatt.getDevice().getAddress());
    }
}
