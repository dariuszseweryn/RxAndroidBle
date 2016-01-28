package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import rx.Observable;
import rx.subjects.BehaviorSubject;

public class RxBleRadioOperationConnect extends RxBleRadioOperation<RxBleConnection> {

    private final Context context;

    private final BluetoothDevice bluetoothDevice;

    private final RxBleGattCallback rxBleGattCallback;

    private final RxBleConnection rxBleConnection;

    private BehaviorSubject<BluetoothGatt> bluetoothGattBehaviorSubject = BehaviorSubject.create();

    public RxBleRadioOperationConnect(Context context, BluetoothDevice bluetoothDevice, RxBleGattCallback rxBleGattCallback,
                                      RxBleConnection rxBleConnection) {
        this.context = context;
        this.bluetoothDevice = bluetoothDevice;
        this.rxBleGattCallback = rxBleGattCallback;
        this.rxBleConnection = rxBleConnection;
    }

    @Override
    public void run() {
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

        bluetoothGattBehaviorSubject.onNext(bluetoothDevice.connectGatt(context, false, rxBleGattCallback.getBluetoothGattCallback()));
    }

    public Observable<BluetoothGatt> getBluetoothGatt() {
        return bluetoothGattBehaviorSubject;
    }
}
