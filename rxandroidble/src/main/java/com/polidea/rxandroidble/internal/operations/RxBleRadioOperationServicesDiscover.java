package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import rx.Subscription;

public class RxBleRadioOperationServicesDiscover extends RxBleRadioOperation<RxBleDeviceServices> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    public RxBleRadioOperationServicesDiscover(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
    }

    @Override
    protected void protectedRun() {

        //noinspection Convert2MethodRef
        final Subscription subscription = rxBleGattCallback
                .getOnServicesDiscovered()
                .first()
                .doOnTerminate(() -> releaseRadio())
                .subscribe(getSubscriber());

        final boolean success = bluetoothGatt.discoverServices();
        if (!success) {
            subscription.unsubscribe();
            onError(new BleGattCannotStartException(BleGattOperationType.SERVICE_DISCOVERY));
        }
    }
}
