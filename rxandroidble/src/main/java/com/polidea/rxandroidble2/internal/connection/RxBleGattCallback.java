package com.polidea.rxandroidble2.internal.connection;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import com.jakewharton.rxrelay3.PublishRelay;
import com.jakewharton.rxrelay3.Relay;
import com.polidea.rxandroidble2.ConnectionParameters;
import com.polidea.rxandroidble2.HiddenBluetoothGattCallback;
import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.RxBleConnection.RxBleConnectionState;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.exceptions.BleGattCharacteristicException;
import com.polidea.rxandroidble2.exceptions.BleGattDescriptorException;
import com.polidea.rxandroidble2.exceptions.BleGattException;
import com.polidea.rxandroidble2.exceptions.BleGattOperationType;
import com.polidea.rxandroidble2.internal.logger.LoggerUtil;
import com.polidea.rxandroidble2.internal.util.ByteAssociation;
import com.polidea.rxandroidble2.internal.util.CharacteristicChangedEvent;

import java.util.UUID;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.functions.Function;
import java.util.concurrent.TimeUnit;


@ConnectionScope
public class RxBleGattCallback {

    private final Scheduler callbackScheduler;
    final BluetoothGattProvider bluetoothGattProvider;
    final DisconnectionRouter disconnectionRouter;
    final NativeCallbackDispatcher nativeCallbackDispatcher;
    final PublishRelay<RxBleConnectionState> connectionStatePublishRelay = PublishRelay.create();
    final Output<RxBleDeviceServices> servicesDiscoveredOutput = new Output<>();
    final Output<ByteAssociation<UUID>> readCharacteristicOutput = new Output<>();
    final Output<ByteAssociation<UUID>> writeCharacteristicOutput = new Output<>();
    final Relay<CharacteristicChangedEvent>
            changedCharacteristicSerializedPublishRelay = PublishRelay.<CharacteristicChangedEvent>create().toSerialized();
    final Output<ByteAssociation<BluetoothGattDescriptor>> readDescriptorOutput = new Output<>();
    final Output<ByteAssociation<BluetoothGattDescriptor>> writeDescriptorOutput = new Output<>();
    final Output<Integer> readRssiOutput = new Output<>();
    final Output<Integer> changedMtuOutput = new Output<>();
    final Output<ConnectionParameters> updatedConnectionOutput = new Output<>();
    private final Function<BleGattException, Observable<?>> errorMapper = new Function<BleGattException, Observable<?>>() {
        @Override
        public Observable<?> apply(BleGattException bleGattException) {
            return Observable.error(bleGattException);
        }
    };

    @Inject
    public RxBleGattCallback(@Named(ClientComponent.NamedSchedulers.BLUETOOTH_CALLBACKS) Scheduler callbackScheduler,
                             BluetoothGattProvider bluetoothGattProvider,
                             DisconnectionRouter disconnectionRouter,
                             NativeCallbackDispatcher nativeCallbackDispatcher) {
        this.callbackScheduler = callbackScheduler;
        this.bluetoothGattProvider = bluetoothGattProvider;
        this.disconnectionRouter = disconnectionRouter;
        this.nativeCallbackDispatcher = nativeCallbackDispatcher;
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            LoggerUtil.logCallback("onConnectionStateChange", gatt, status, newState);
            nativeCallbackDispatcher.notifyNativeConnectionStateCallback(gatt, status, newState);
            super.onConnectionStateChange(gatt, status, newState);
            bluetoothGattProvider.updateBluetoothGatt(gatt);

            if (isDisconnectedOrDisconnecting(newState)) {
                disconnectionRouter.onDisconnectedException(new BleDisconnectedException(gatt.getDevice().getAddress(), status));
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectionRouter.onGattConnectionStateException(
                        new BleGattException(gatt, status, BleGattOperationType.CONNECTION_STATE)
                );
            }

            connectionStatePublishRelay.accept(mapConnectionStateToRxBleConnectionStatus(newState));
        }

        private boolean isDisconnectedOrDisconnecting(int newState) {
            return newState == BluetoothGatt.STATE_DISCONNECTED || newState == BluetoothGatt.STATE_DISCONNECTING;
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            LoggerUtil.logCallback("onServicesDiscovered", gatt, status);
            nativeCallbackDispatcher.notifyNativeServicesDiscoveredCallback(gatt, status);
            super.onServicesDiscovered(gatt, status);

            if (servicesDiscoveredOutput.hasObservers()
                    && !propagateErrorIfOccurred(servicesDiscoveredOutput, gatt, status, BleGattOperationType.SERVICE_DISCOVERY)) {
                servicesDiscoveredOutput.valueRelay.accept(new RxBleDeviceServices(gatt.getServices()));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            LoggerUtil.logCallback("onCharacteristicRead", gatt, status, characteristic, true);
            nativeCallbackDispatcher.notifyNativeReadCallback(gatt, characteristic, status);
            super.onCharacteristicRead(gatt, characteristic, status);

            if (readCharacteristicOutput.hasObservers() && !propagateErrorIfOccurred(
                    readCharacteristicOutput, gatt, characteristic, status, BleGattOperationType.CHARACTERISTIC_READ
            )) {
                readCharacteristicOutput.valueRelay.accept(new ByteAssociation<>(characteristic.getUuid(), characteristic.getValue()));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            LoggerUtil.logCallback("onCharacteristicWrite", gatt, status, characteristic, false);
            nativeCallbackDispatcher.notifyNativeWriteCallback(gatt, characteristic, status);
            super.onCharacteristicWrite(gatt, characteristic, status);

            if (writeCharacteristicOutput.hasObservers() && !propagateErrorIfOccurred(
                    writeCharacteristicOutput, gatt, characteristic, status, BleGattOperationType.CHARACTERISTIC_WRITE
            )) {
                writeCharacteristicOutput.valueRelay.accept(new ByteAssociation<>(characteristic.getUuid(), characteristic.getValue()));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            LoggerUtil.logCallback("onCharacteristicChanged", gatt, characteristic, true);
            nativeCallbackDispatcher.notifyNativeChangedCallback(gatt, characteristic);
            super.onCharacteristicChanged(gatt, characteristic);

            /*
             * It is important to call changedCharacteristicSerializedPublishRelay as soon as possible because a quick changing
             * characteristic could lead to out-of-order execution since onCharacteristicChanged may be called on arbitrary
             * threads.
             */
            if (changedCharacteristicSerializedPublishRelay.hasObservers()) {
                changedCharacteristicSerializedPublishRelay.accept(
                        new CharacteristicChangedEvent(
                                characteristic.getUuid(),
                                characteristic.getInstanceId(),
                                characteristic.getValue()
                        )
                );
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            LoggerUtil.logCallback("onDescriptorRead", gatt, status, descriptor, true);
            nativeCallbackDispatcher.notifyNativeDescriptorReadCallback(gatt, descriptor, status);
            super.onDescriptorRead(gatt, descriptor, status);

            if (readDescriptorOutput.hasObservers()
                    && !propagateErrorIfOccurred(readDescriptorOutput, gatt, descriptor, status, BleGattOperationType.DESCRIPTOR_READ)) {
                readDescriptorOutput.valueRelay.accept(new ByteAssociation<>(descriptor, descriptor.getValue()));
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            LoggerUtil.logCallback("onDescriptorWrite", gatt, status, descriptor, false);
            nativeCallbackDispatcher.notifyNativeDescriptorWriteCallback(gatt, descriptor, status);
            super.onDescriptorWrite(gatt, descriptor, status);

            if (writeDescriptorOutput.hasObservers()
                    && !propagateErrorIfOccurred(writeDescriptorOutput, gatt, descriptor, status, BleGattOperationType.DESCRIPTOR_WRITE)) {
                writeDescriptorOutput.valueRelay.accept(new ByteAssociation<>(descriptor, descriptor.getValue()));
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            LoggerUtil.logCallback("onReliableWriteCompleted", gatt, status);
            nativeCallbackDispatcher.notifyNativeReliableWriteCallback(gatt, status);
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            LoggerUtil.logCallback("onReadRemoteRssi", gatt, status, rssi);
            nativeCallbackDispatcher.notifyNativeReadRssiCallback(gatt, rssi, status);
            super.onReadRemoteRssi(gatt, rssi, status);

            if (readRssiOutput.hasObservers()
                    && !propagateErrorIfOccurred(readRssiOutput, gatt, status, BleGattOperationType.READ_RSSI)) {
                readRssiOutput.valueRelay.accept(rssi);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            LoggerUtil.logCallback("onMtuChanged", gatt, status, mtu);
            nativeCallbackDispatcher.notifyNativeMtuChangedCallback(gatt, mtu, status);
            super.onMtuChanged(gatt, mtu, status);

            if (changedMtuOutput.hasObservers()
                    && !propagateErrorIfOccurred(changedMtuOutput, gatt, status, BleGattOperationType.ON_MTU_CHANGED)) {
                changedMtuOutput.valueRelay.accept(mtu);
            }
        }

        // This callback first appeared in Android 8.0 (android-8.0.0_r1/core/java/android/bluetooth/BluetoothGattCallback.java)
        // It is hidden since
        @SuppressWarnings("unused")
        public void onConnectionUpdated(BluetoothGatt gatt, int interval, int latency, int timeout, int status) {
            LoggerUtil.logConnectionUpdateCallback("onConnectionUpdated", gatt, status, interval, latency, timeout);
            nativeCallbackDispatcher.notifyNativeParamsUpdateCallback(gatt, interval, latency, timeout, status);
            if (updatedConnectionOutput.hasObservers()
                    && !propagateErrorIfOccurred(updatedConnectionOutput, gatt, status, BleGattOperationType.CONNECTION_PRIORITY_CHANGE)) {
                updatedConnectionOutput.valueRelay.accept(new ConnectionParametersImpl(interval, latency, timeout));
            }
        }
    };

    static RxBleConnectionState mapConnectionStateToRxBleConnectionStatus(int newState) {

        switch (newState) {
            case BluetoothGatt.STATE_CONNECTING:
                return RxBleConnectionState.CONNECTING;
            case BluetoothGatt.STATE_CONNECTED:
                return RxBleConnectionState.CONNECTED;
            case BluetoothGatt.STATE_DISCONNECTING:
                return RxBleConnectionState.DISCONNECTING;
            default:
                return RxBleConnectionState.DISCONNECTED;
        }
    }

    static boolean propagateErrorIfOccurred(
            Output<?> output,
            BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic,
            int status,
            BleGattOperationType operationType
    ) {
        return isException(status) && propagateStatusError(output, new BleGattCharacteristicException(
                gatt,
                characteristic,
                status,
                operationType
        ));
    }

    static boolean propagateErrorIfOccurred(
            Output<?> output,
            BluetoothGatt gatt,
            BluetoothGattDescriptor descriptor,
            int status,
            BleGattOperationType operationType
    ) {
        return isException(status) && propagateStatusError(output, new BleGattDescriptorException(
                gatt,
                descriptor,
                status,
                operationType
        ));
    }

    static boolean propagateErrorIfOccurred(Output<?> output, BluetoothGatt gatt, int status, BleGattOperationType operationType) {
        return isException(status) && propagateStatusError(output, new BleGattException(gatt, status, operationType));
    }

    private static boolean isException(int status) {
        return status != BluetoothGatt.GATT_SUCCESS;
    }

    private static boolean propagateStatusError(Output<?> output, BleGattException exception) {
        output.errorRelay.accept(exception);
        return true;
    }

    @SuppressWarnings("unchecked")
    private <T> Observable<T> withDisconnectionHandling(Output<T> output) {
        return Observable.merge(
                disconnectionRouter.<T>asErrorOnlyObservable(),
                output.valueRelay,
                (Observable<T>) output.errorRelay.flatMap(errorMapper)
        );
    }

    public BluetoothGattCallback getBluetoothGattCallback() {
        return bluetoothGattCallback;
    }

    /**
     * @return Observable that never emits onNext.
     * @throws BleDisconnectedException emitted in case of a disconnect that is a part of the normal flow
     * @throws BleGattException         emitted in case of connection was interrupted unexpectedly.
     */
    public <T> Observable<T> observeDisconnect() {
        return disconnectionRouter.asErrorOnlyObservable();
    }

    /**
     * @return Observable that emits RxBleConnectionState that matches BluetoothGatt's state.
     * Does NOT emit errors even if status != GATT_SUCCESS.
     */
    public Observable<RxBleConnectionState> getOnConnectionStateChange() {
        return connectionStatePublishRelay.delay(0, TimeUnit.SECONDS, callbackScheduler);
    }

    public Observable<RxBleDeviceServices> getOnServicesDiscovered() {
        return withDisconnectionHandling(servicesDiscoveredOutput).delay(0, TimeUnit.SECONDS, callbackScheduler);
    }

    public Observable<Integer> getOnMtuChanged() {
        return withDisconnectionHandling(changedMtuOutput).delay(0, TimeUnit.SECONDS, callbackScheduler);
    }

    public Observable<ByteAssociation<UUID>> getOnCharacteristicRead() {
        return withDisconnectionHandling(readCharacteristicOutput).delay(0, TimeUnit.SECONDS, callbackScheduler);
    }

    public Observable<ByteAssociation<UUID>> getOnCharacteristicWrite() {
        return withDisconnectionHandling(writeCharacteristicOutput).delay(0, TimeUnit.SECONDS, callbackScheduler);
    }

    public Observable<CharacteristicChangedEvent> getOnCharacteristicChanged() {
        return Observable.merge(
                disconnectionRouter.<CharacteristicChangedEvent>asErrorOnlyObservable(),
                changedCharacteristicSerializedPublishRelay
        )
                .delay(0, TimeUnit.SECONDS, callbackScheduler);
    }

    public Observable<ByteAssociation<BluetoothGattDescriptor>> getOnDescriptorRead() {
        return withDisconnectionHandling(readDescriptorOutput).delay(0, TimeUnit.SECONDS, callbackScheduler);
    }

    public Observable<ByteAssociation<BluetoothGattDescriptor>> getOnDescriptorWrite() {
        return withDisconnectionHandling(writeDescriptorOutput).delay(0, TimeUnit.SECONDS, callbackScheduler);
    }

    public Observable<Integer> getOnRssiRead() {
        return withDisconnectionHandling(readRssiOutput).delay(0, TimeUnit.SECONDS, callbackScheduler);
    }

    public Observable<ConnectionParameters> getConnectionParametersUpdates() {
        return withDisconnectionHandling(updatedConnectionOutput).delay(0, TimeUnit.SECONDS, callbackScheduler);
    }

    /**
     * A native callback allows to omit RxJava's abstraction on the {@link BluetoothGattCallback}.
     * It's intended to be used only with a {@link com.polidea.rxandroidble2.RxBleCustomOperation} in a performance
     * critical implementations. If you don't know if your operation is performance critical it's likely that you shouldn't use this API
     * and stick with the RxJava.
     * <p>
     * The callback reference will be automatically released after the operation is terminated. The main drawback of this API is that
     * we can't assure you the thread on which it will be executed. Please keep this in mind as the system may execute it on a main thread.
     *
     * @param callback the object to be called
     */
    public void setNativeCallback(BluetoothGattCallback callback) {
        nativeCallbackDispatcher.setNativeCallback(callback);
    }

    /**
     * {@link #setNativeCallback(BluetoothGattCallback)}
     * Since Android 8.0 (API 26) BluetoothGattCallback has some hidden method(s). Setting this {@link HiddenBluetoothGattCallback} will
     * relay calls to those hidden methods.
     *
     * On API lower than 26 this method does nothing
     *
     * @param callbackHidden the object to be called
     */
    public void setHiddenNativeCallback(HiddenBluetoothGattCallback callbackHidden) {
        nativeCallbackDispatcher.setNativeCallbackHidden(callbackHidden);
    }

    private static class Output<T> {

        final PublishRelay<T> valueRelay;
        final PublishRelay<BleGattException> errorRelay;

        Output() {
            this.valueRelay = PublishRelay.create();
            this.errorRelay = PublishRelay.create();
        }

        boolean hasObservers() {
            return valueRelay.hasObservers() || errorRelay.hasObservers();
        }
    }
}
