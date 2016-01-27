package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.util.Log;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RxBleRadioOperationServicesDiscover extends RxBleRadioOperation<Map<UUID,Set<UUID>>> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    public RxBleRadioOperationServicesDiscover(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        Log.d("xxx", "constructor " + bluetoothGatt);
    }

    @Override
    public void run() {
        rxBleGattCallback
                .getOnServicesDiscovered()
                .take(1)
                .doOnNext(uuidSetMap -> releaseRadio())
                .subscribe(getSubscriber());

        final boolean success = bluetoothGatt.discoverServices();
        if (!success) {
            onError(new BleScanException(BleScanException.BLE_CANNOT_START));
        }
    }
}
