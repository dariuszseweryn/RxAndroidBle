package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;

import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import rx.Subscription;

public class RxBleRadioOperationCharacteristicWrite extends RxBleRadioOperation<byte[]> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final BluetoothGattCharacteristic bluetoothGattCharacteristic;

    private final byte[] data;

    private Integer mtu;

    public RxBleRadioOperationCharacteristicWrite(
            RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
            BluetoothGattCharacteristic bluetoothGattCharacteristic, byte[] data) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.data = data;
    }

    public RxBleRadioOperationCharacteristicWrite(
            RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
            BluetoothGattCharacteristic bluetoothGattCharacteristic, byte[] data, int mtu) {
        this(rxBleGattCallback, bluetoothGatt, bluetoothGattCharacteristic, data);
        this.mtu = mtu;

    }

    @Override
    protected void protectedRun() {
        if (data.length > 20) {
            // Enable long write
            RxBleLog.d("Enabling long write on this characteristic");
            bluetoothGattCharacteristic.setWriteType(
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        }

        // MTU change is only allowed on API 21+ and up
        if (mtu != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            rxBleGattCallback.getOnMtuChanged()
                    .subscribe(mtu -> {
                        RxBleLog.d("write:: mtu changed");
                        writePayload();
                    });
            bluetoothGatt.requestMtu(mtu);
        } else {
            writePayload();
        }
    }

    private void writePayload() {
        final Subscription subscription = rxBleGattCallback
                .getOnCharacteristicWrite()
                .filter(uuidPair -> uuidPair.first.equals(bluetoothGattCharacteristic.getUuid()))
                .take(1)
                .map(uuidPair -> uuidPair.second)
                .doOnCompleted(this::releaseRadio)
                .subscribe(getSubscriber());

        //noinspection Convert2MethodRef
        bluetoothGattCharacteristic.setValue(data);
        final boolean success = bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        if (!success) {
            subscription.unsubscribe();
            onError(new BleGattCannotStartException(bluetoothGatt, BleGattOperationType.CHARACTERISTIC_WRITE));
        }
    }
}
