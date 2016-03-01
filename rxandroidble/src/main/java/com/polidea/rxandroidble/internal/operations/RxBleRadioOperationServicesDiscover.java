package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleGattException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import java.util.concurrent.atomic.AtomicReference;
import rx.Observable;
import rx.Subscription;

public class RxBleRadioOperationServicesDiscover extends RxBleRadioOperation<RxBleDeviceServices> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    public RxBleRadioOperationServicesDiscover(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
    }

    @Override
    public void run() {

        //noinspection Convert2MethodRef
        Observable.<RxBleDeviceServices>create(subscriber -> {

                    final Subscription subscription = rxBleGattCallback
                            .getOnServicesDiscovered()
                            .first()
                            .subscribe(subscriber);

                    final boolean success = bluetoothGatt.discoverServices();
                    if (!success) {
                        subscription.unsubscribe();
                        subscriber.onError(new BleGattException(BleGattOperationType.SERVICE_DISCOVERY));
                    }
                }
        )
                .doOnTerminate(() -> releaseRadio())
                .subscribe(getSubscriber());
    }
}
