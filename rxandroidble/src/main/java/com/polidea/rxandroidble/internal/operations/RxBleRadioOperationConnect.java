package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;

public class RxBleRadioOperationConnect extends RxBleRadioOperation<RxBleConnection> {

    private final Context context;

    private final BluetoothDevice bluetoothDevice;

    private final RxBleGattCallback rxBleGattCallback;

    private final RxBleConnection rxBleConnection;

    private BluetoothGatt bluetoothGatt;

    public RxBleRadioOperationConnect(Context context, BluetoothDevice bluetoothDevice, RxBleGattCallback rxBleGattCallback,
                                      RxBleConnection rxBleConnection) {
        this.context = context;
        this.bluetoothDevice = bluetoothDevice;
        this.rxBleGattCallback = rxBleGattCallback;
        this.rxBleConnection = rxBleConnection;
    }

    @Override
    public void run() {
        rxBleGattCallback
                .getOnConnectionStateChange()
                .filter(rxBleConnectionState -> rxBleConnectionState == RxBleConnection.RxBleConnectionState.CONNECTED)
                .subscribe(
                        rxBleConnectionState -> {
                            onNext(rxBleConnection);
                            releaseRadio();
                        },
                        this::onError,
                        this::onCompleted
                );

        bluetoothGatt = bluetoothDevice.connectGatt(context, false, rxBleGattCallback.getBluetoothGattCallback());
    }

    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }
}
