package com.polidea.rxandroidble.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.os.DeadObjectException;

import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.internal.DeviceModule;
import com.polidea.rxandroidble.internal.serialization.QueueReleaseInterface;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.QueueOperation;
import com.polidea.rxandroidble.internal.connection.BluetoothGattProvider;
import com.polidea.rxandroidble.internal.connection.ConnectionStateChangeListener;
import com.polidea.rxandroidble.internal.connection.RxBleGattCallback;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Emitter;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.DISCONNECTED;
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.DISCONNECTING;
import static rx.Observable.just;

public class DisconnectOperation extends QueueOperation<Void> {

    private final RxBleGattCallback rxBleGattCallback;
    private final BluetoothGattProvider bluetoothGattProvider;
    private final String macAddress;
    private final BluetoothManager bluetoothManager;
    private final Scheduler bluetoothInteractionScheduler;
    private final TimeoutConfiguration timeoutConfiguration;
    private final ConnectionStateChangeListener connectionStateChangeListener;

    @Inject
    DisconnectOperation(
            RxBleGattCallback rxBleGattCallback,
            BluetoothGattProvider bluetoothGattProvider,
            @Named(DeviceModule.MAC_ADDRESS) String macAddress,
            BluetoothManager bluetoothManager,
            @Named(ClientComponent.NamedSchedulers.BLUETOOTH_INTERACTION) Scheduler bluetoothInteractionScheduler,
            @Named(DeviceModule.DISCONNECT_TIMEOUT) TimeoutConfiguration timeoutConfiguration,
            ConnectionStateChangeListener connectionStateChangeListener) {
        this.rxBleGattCallback = rxBleGattCallback;
        this.bluetoothGattProvider = bluetoothGattProvider;
        this.macAddress = macAddress;
        this.bluetoothManager = bluetoothManager;
        this.bluetoothInteractionScheduler = bluetoothInteractionScheduler;
        this.timeoutConfiguration = timeoutConfiguration;
        this.connectionStateChangeListener = connectionStateChangeListener;
    }

    @Override
    protected void protectedRun(final Emitter<Void> emitter, final QueueReleaseInterface queueReleaseInterface) {
        connectionStateChangeListener.onConnectionStateChange(DISCONNECTING);
        final BluetoothGatt bluetoothGatt = bluetoothGattProvider.getBluetoothGatt();

        if (bluetoothGatt == null) {
            RxBleLog.w("Disconnect operation has been executed but GATT instance was null - considering disconnected.");
            considerGattDisconnected(emitter, queueReleaseInterface);
        } else {
            (isDisconnected(bluetoothGatt) ? just(bluetoothGatt) : disconnect(bluetoothGatt))
                    .observeOn(bluetoothInteractionScheduler)
                    .subscribe(new Observer<BluetoothGatt>() {
                        @Override
                        public void onNext(BluetoothGatt bluetoothGatt) {
                            bluetoothGatt.close();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            RxBleLog.w(
                                    throwable,
                                    "Disconnect operation has been executed but finished with an error - considering disconnected."
                            );
                            considerGattDisconnected(emitter, queueReleaseInterface);
                        }

                        @Override
                        public void onCompleted() {
                            considerGattDisconnected(emitter, queueReleaseInterface);
                        }
                    });
        }
    }

    @SuppressWarnings("WeakerAccess")
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    void considerGattDisconnected(
            final Emitter<Void> emitter,
            final QueueReleaseInterface queueReleaseInterface
    ) {
        connectionStateChangeListener.onConnectionStateChange(DISCONNECTED);
        queueReleaseInterface.release();
        emitter.onCompleted();
    }

    private boolean isDisconnected(BluetoothGatt bluetoothGatt) {
        return bluetoothManager.getConnectionState(bluetoothGatt.getDevice(), BluetoothProfile.GATT) == BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * TODO: [DS] 09.02.2016 This operation makes the queue to block until disconnection - maybe it would be better if it would not?
     * What would happen then if a consecutive call to BluetoothDevice.connectGatt() would be made? What BluetoothGatt would be returned?
     * 1. A completely fresh BluetoothGatt - would work with the current flow
     * 2. The same BluetoothGatt - in this situation we should probably cancel the pending BluetoothGatt.close() call
     */
    private Observable<BluetoothGatt> disconnect(BluetoothGatt bluetoothGatt) {
        return new DisconnectGattObservable(bluetoothGatt, rxBleGattCallback, bluetoothInteractionScheduler)
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
                            .takeFirst(new Func1<RxBleConnection.RxBleConnectionState, Boolean>() {
                                @Override
                                public Boolean call(RxBleConnection.RxBleConnectionState rxBleConnectionState) {
                                    return rxBleConnectionState == DISCONNECTED;
                                }
                            })
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
