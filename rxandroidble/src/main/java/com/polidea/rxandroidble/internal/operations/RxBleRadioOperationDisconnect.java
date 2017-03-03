package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.os.DeadObjectException;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.internal.DeviceModule;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.connection.BluetoothGattProvider;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import static rx.Observable.just;

public class RxBleRadioOperationDisconnect extends RxBleRadioOperation<Void> {

    private final RxBleGattCallback rxBleGattCallback;
    private final BluetoothGattProvider bluetoothGattProvider;
    private final String macAddress;
    private final BluetoothManager bluetoothManager;
    private final Scheduler mainThreadScheduler;
    private final TimeoutConfiguration timeoutConfiguration;

    @Inject
    RxBleRadioOperationDisconnect(
            RxBleGattCallback rxBleGattCallback,
            BluetoothGattProvider bluetoothGattProvider,
            @Named(DeviceModule.MAC_ADDRESS) String macAddress,
            BluetoothManager bluetoothManager,
            @Named(ClientComponent.NamedSchedulers.MAIN_THREAD) Scheduler mainThreadScheduler,
            @Named(DeviceModule.DISCONNECT_TIMEOUT) TimeoutConfiguration timeoutConfiguration) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGattProvider = bluetoothGattProvider;
        this.macAddress = macAddress;
        this.bluetoothManager = bluetoothManager;
        this.mainThreadScheduler = mainThreadScheduler;
        this.timeoutConfiguration = timeoutConfiguration;
    }

    @Override
    protected void protectedRun() {
        //noinspection Convert2MethodRef
        final BluetoothGatt bluetoothGatt = bluetoothGattProvider.getBluetoothGatt();

        if (bluetoothGatt == null) {
            RxBleLog.w("Disconnect operation has been executed but GATT instance was null.");
            onCompleted();
        } else {
            (isDisconnected(bluetoothGatt) ? just(bluetoothGatt) : disconnect(bluetoothGatt))
                    .doOnTerminate(new Action0() {
                        @Override
                        public void call() {
                            releaseRadio();
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
                                    onError(throwable);
                                }
                            },
                            new Action0() {
                                @Override
                                public void call() {
                                    onCompleted();
                                }
                            }
                    );
        }
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
                .timeout(timeoutConfiguration.timeout, timeoutConfiguration.timeoutTimeUnit, just(bluetoothGatt),
                        timeoutConfiguration.timeoutScheduler);
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
