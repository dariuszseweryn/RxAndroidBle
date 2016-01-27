package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;

public class RxBleRadioOperationDisconnect extends RxBleRadioOperation<Void> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final BluetoothManager bluetoothManager;

    public RxBleRadioOperationDisconnect(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt, BluetoothManager bluetoothManager) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.bluetoothManager = bluetoothManager;
    }

    @Override
    public void run() {

        if (bluetoothManager.getConnectionState(bluetoothGatt.getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_DISCONNECTED) {
            bluetoothGatt.close();
            onNext(null);
            releaseRadio();
            return;
        }

        //noinspection Convert2MethodRef
        rxBleGattCallback
                .getOnConnectionStateChange()
                .filter(rxBleConnectionState -> rxBleConnectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED)
                .take(1)
                .subscribe(
                        rxBleConnectionState -> {
                            bluetoothGatt.close();
                            onNext(null);
                            releaseRadio();
                        },
                        throwable -> onError(throwable),
                        () -> onCompleted() // don't change to method reference - crash on with Retrolambda 3.2.4
                );

        bluetoothGatt.disconnect(); // TODO check if needed to disconnect
    }
}
