package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;

public class RxBleRadioOperationConnect extends RxBleRadioOperation<RxBleConnection> {

    private final Context context;

    private final BluetoothDevice bluetoothDevice;

    private final RxBleGattCallback rxBleGattCallback;

    private final RxBleConnection rxBleConnection;

    private BehaviorSubject<BluetoothGatt> bluetoothGattBehaviorSubject = BehaviorSubject.create();

    private Subscription bluetoothGattSubscription;

    public RxBleRadioOperationConnect(Context context, BluetoothDevice bluetoothDevice, RxBleGattCallback rxBleGattCallback,
                                      RxBleConnection rxBleConnection) {
        this.context = context;
        this.bluetoothDevice = bluetoothDevice;
        this.rxBleGattCallback = rxBleGattCallback;
        this.rxBleConnection = rxBleConnection;
    }

    @Override
    public Observable<RxBleConnection> asObservable() {
        return super.asObservable()
                .doOnUnsubscribe(() -> {
                    if (bluetoothGattSubscription != null) {
                        bluetoothGattSubscription.unsubscribe();
                    }
                });
    }

    @Override
    public void run() {
        // TODO: [PU] 29.01.2016 If may happen that disconnect callback will be executed even before connect. Will be stuck here?
        // TODO: [DS] 09.02.2016 True - we need to fix that
        //noinspection Convert2MethodRef
        rxBleGattCallback
                .getOnConnectionStateChange()
                .filter(rxBleConnectionState -> rxBleConnectionState == RxBleConnection.RxBleConnectionState.CONNECTED)
                .subscribe(
                        rxBleConnectionState -> {
                            onNext(rxBleConnection);
                            releaseRadio();
                        },
                        (throwable) -> {
                            onError(throwable);
                            bluetoothGattBehaviorSubject.onCompleted();
                        },
                        () -> {
                            onCompleted();
                            bluetoothGattBehaviorSubject.onCompleted();
                        }
                );

        bluetoothGattSubscription = rxBleGattCallback.getBluetoothGatt().subscribe(bluetoothGattBehaviorSubject::onNext);
        // TODO: [PU] 29.01.2016 We must consider background connection with autoconnect == true.
        final BluetoothGatt bluetoothGatt = bluetoothDevice.connectGatt(context, false, rxBleGattCallback.getBluetoothGattCallback());
        bluetoothGattBehaviorSubject.onNext(bluetoothGatt);
    }

    public Observable<BluetoothGatt> getBluetoothGatt() {
        return bluetoothGattBehaviorSubject;
    }
}
