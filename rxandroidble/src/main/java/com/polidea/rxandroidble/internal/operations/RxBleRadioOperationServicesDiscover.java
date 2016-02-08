package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleGattException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import java.util.concurrent.atomic.AtomicReference;
import rx.Observable;

public class RxBleRadioOperationServicesDiscover extends RxBleRadioOperation<RxBleDeviceServices> {

    private final RxBleGattCallback rxBleGattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final AtomicReference<RxBleDeviceServices> rxBleDeviceServicesCache;

    public RxBleRadioOperationServicesDiscover(RxBleGattCallback rxBleGattCallback, BluetoothGatt bluetoothGatt,
                                               AtomicReference<RxBleDeviceServices> rxBleDeviceServicesCache) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.rxBleDeviceServicesCache = rxBleDeviceServicesCache;
    }

    @Override
    public void run() {

        //noinspection Convert2MethodRef
        Observable.just(rxBleDeviceServicesCache.get())
                .flatMap(rxBleDeviceServices -> {
                    if (rxBleDeviceServices != null) {
                        return Observable.just(rxBleDeviceServices);
                    } else {
                        return Observable.empty();
                    }
                })
                .switchIfEmpty(Observable.create(subscriber -> {

                            final boolean success = bluetoothGatt.discoverServices();
                            if (!success) {
                                subscriber.onError(new BleGattException(BleGattOperationType.SERVICE_DISCOVERY));
                                return;
                            }

                            rxBleGattCallback
                                    .getOnServicesDiscovered()
                                    .first()
                                    .doOnNext(rxBleDeviceServicesCache::set)
                                    .subscribe(subscriber);
                        }
                ))
                .doOnTerminate(() -> releaseRadio())
                .subscribe(getSubscriber());
    }
}
