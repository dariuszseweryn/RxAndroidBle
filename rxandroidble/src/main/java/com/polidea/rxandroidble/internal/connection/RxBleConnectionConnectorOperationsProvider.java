package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.support.v4.util.Pair;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect;
import java.util.concurrent.atomic.AtomicReference;

public class RxBleConnectionConnectorOperationsProvider {

    public Pair<RxBleRadioOperationConnect, RxBleRadioOperationDisconnect> provide(Context context, BluetoothDevice bluetoothDevice, boolean autoConnect,
                                                                                   RxBleGattCallback gattCallback) {
        final AtomicReference<BluetoothGatt> bluetoothGattAtomicReference = new AtomicReference<>();

        final RxBleRadioOperationConnect operationConnect = new RxBleRadioOperationConnect(context, bluetoothDevice, gattCallback, autoConnect);
        final RxBleRadioOperationDisconnect operationDisconnect = new RxBleRadioOperationDisconnect(
                gattCallback,
                bluetoothGattAtomicReference,
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)
        );

        operationConnect.getBluetoothGatt().first().subscribe(bluetoothGattAtomicReference::set, ignored -> {});

        return new Pair<>(operationConnect, operationDisconnect);
    }
}
