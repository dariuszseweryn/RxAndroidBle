package com.polidea.rxandroidble.internal.operations;

import static rx.Observable.just;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.os.DeadObjectException;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

public class RxBleRadioOperationDisconnect extends RxBleRadioOperation<Void> {

    private static final int TIMEOUT_DISCONNECT = 10;
    private final RxBleGattCallback rxBleGattCallback;
    private final String macAddress;
    private final AtomicReference<BluetoothGatt> bluetoothGattAtomicReference;
    private final BluetoothManager bluetoothManager;
    private final Scheduler mainThreadScheduler;

    public RxBleRadioOperationDisconnect(RxBleGattCallback rxBleGattCallback, String macAddress,
                                         AtomicReference<BluetoothGatt> bluetoothGattAtomicReference,
                                         BluetoothManager bluetoothManager, Scheduler mainThreadScheduler) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.macAddress = macAddress;
        this.bluetoothGattAtomicReference = bluetoothGattAtomicReference;
        this.bluetoothManager = bluetoothManager;
        this.mainThreadScheduler = mainThreadScheduler;
    }

    @Override
    protected void protectedRun() {
        //noinspection Convert2MethodRef
        just(bluetoothGattAtomicReference.get())
                .filter(new Func1<BluetoothGatt, Boolean>() {
                    @Override
                    public Boolean call(BluetoothGatt bluetoothGatt) {
                        return bluetoothGatt != null;
                    }
                })
                .flatMap(new Func1<BluetoothGatt, Observable<BluetoothGatt>>() {
                    @Override
                    public Observable<BluetoothGatt> call(BluetoothGatt bluetoothGatt) {
                        return RxBleRadioOperationDisconnect.this.isDisconnected(bluetoothGatt)
                                ? just(bluetoothGatt) : RxBleRadioOperationDisconnect.this.disconnect(bluetoothGatt);
                    }
                })
                .doOnTerminate(new Action0() {
                    @Override
                    public void call() {
                        RxBleRadioOperationDisconnect.this.releaseRadio();
                    }
                })
                .observeOn(mainThreadScheduler)
                .subscribe(
                        new Action1<BluetoothGatt>() {
                            @Override
                            public void call(BluetoothGatt bluetoothGatt) {
                                bluetoothGatt.close();
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                RxBleRadioOperationDisconnect.this.onError(throwable);
                            }
                        },
                        new Action0() {
                            @Override
                            public void call() {
                                RxBleRadioOperationDisconnect.this.onCompleted();
                            }
                        }
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
                final BluetoothGatt bluetoothGatt,
                final RxBleGattCallback rxBleGattCallback,
                final Scheduler disconnectScheduler
        ) {
            super(new OnSubscribe<BluetoothGatt>() {
                @Override
                public void call(Subscriber<? super BluetoothGatt> subscriber) {
                    rxBleGattCallback
                            .getOnConnectionStateChange()
                            .filter(new Func1<RxBleConnection.RxBleConnectionState, Boolean>() {
                                @Override
                                public Boolean call(RxBleConnection.RxBleConnectionState rxBleConnectionState) {
                                    return rxBleConnectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED;
                                }
                            })
                            .take(1)
                            .map(new Func1<RxBleConnection.RxBleConnectionState, BluetoothGatt>() {
                                @Override
                                public BluetoothGatt call(RxBleConnection.RxBleConnectionState rxBleConnectionState) {
                                    return bluetoothGatt;
                                }
                            })
                            .subscribe(subscriber);
                    disconnectScheduler.createWorker().schedule(new Action0() {
                        @Override
                        public void call() {
                            bluetoothGatt.disconnect();
                        }
                    });
                }
            });
        }
    }

    @Override
    protected BleException provideException(DeadObjectException deadObjectException) {
        return new BleDisconnectedException(deadObjectException, macAddress);
    }
}
