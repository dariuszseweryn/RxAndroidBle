package com.polidea.rxandroidble.internal.operations;

import static rx.Observable.just;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import rx.Observable;
import rx.Scheduler;

public class RxBleRadioOperationDisconnect extends RxBleRadioOperation<Void> {

    private static final int TIMEOUT_DISCONNECT = 10;
    private final RxBleGattCallback rxBleGattCallback;
    private final AtomicReference<BluetoothGatt> bluetoothGattAtomicReference;
    private final BluetoothManager bluetoothManager;
    private final Scheduler mainThreadScheduler;

    public RxBleRadioOperationDisconnect(RxBleGattCallback rxBleGattCallback, AtomicReference<BluetoothGatt> bluetoothGattAtomicReference,
                                         BluetoothManager bluetoothManager, Scheduler mainThreadScheduler) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGattAtomicReference = bluetoothGattAtomicReference;
        this.bluetoothManager = bluetoothManager;
        this.mainThreadScheduler = mainThreadScheduler;
    }

    @Override
    protected void protectedRun() {
        //noinspection Convert2MethodRef
        just(bluetoothGattAtomicReference.get())
                .filter(bluetoothGatt -> bluetoothGatt != null)
                .flatMap(bluetoothGatt -> isDisconnected(bluetoothGatt) ? just(bluetoothGatt) : disconnect(bluetoothGatt))
                .doOnTerminate(() -> releaseRadio())
                .observeOn(mainThreadScheduler)
                .subscribe(
                        bluetoothGatt -> bluetoothGatt.close(),
                        throwable -> onError(throwable),
                        () -> onCompleted()
                );
    }

    private boolean isDisconnected(BluetoothGatt bluetoothGatt) {
        return bluetoothManager.getConnectionState(bluetoothGatt.getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * TODO: [DS] 09.02.2016 This operation makes the radio to block until disconnection - maybe it would be better if it would not?
     * What would happen then if a consecutive call to BluetoothDevice.connectGatt() would be made? What BluetoothGatt would be returned?
     * 1. A completely fresh BluetoothGatt - would work with the current flow
     * 2. The same BluetoothGatt - in this situation we should probably cancel the pending BluetoothGatt.close() call
     */
    private Observable<BluetoothGatt> disconnect(BluetoothGatt bluetoothGatt) {
        return new DisconnectGattObservable(bluetoothGatt, rxBleGattCallback, mainThreadScheduler)
                .timeout(TIMEOUT_DISCONNECT, TimeUnit.SECONDS, just(bluetoothGatt));
    }

    private static class DisconnectGattObservable extends Observable<BluetoothGatt> {

        DisconnectGattObservable(
                BluetoothGatt bluetoothGatt,
                RxBleGattCallback rxBleGattCallback,
                Scheduler disconnectScheduler
        ) {
            super(subscriber -> {
                rxBleGattCallback
                        .getOnConnectionStateChange()
                        .filter(rxBleConnectionState -> rxBleConnectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED)
                        .take(1)
                        .map(rxBleConnectionState -> bluetoothGatt)
                        .subscribe(subscriber);
                disconnectScheduler.createWorker().schedule(bluetoothGatt::disconnect);
            });
        }
    }
}
