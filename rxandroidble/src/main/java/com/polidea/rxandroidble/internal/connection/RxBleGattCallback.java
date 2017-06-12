package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Pair;

import com.jakewharton.rxrelay.PublishRelay;
import com.jakewharton.rxrelay.SerializedRelay;
import com.polidea.rxandroidble.ClientComponent;

import com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleGattCharacteristicException;
import com.polidea.rxandroidble.exceptions.BleGattDescriptorException;
import com.polidea.rxandroidble.exceptions.BleGattException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleLog;
import com.polidea.rxandroidble.internal.util.ByteAssociation;
import com.polidea.rxandroidble.internal.util.CharacteristicChangedEvent;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

@ConnectionScope
public class RxBleGattCallback {

    private final Scheduler callbackScheduler;
    private final BluetoothGattProvider bluetoothGattProvider;
    private final Output<Pair<BluetoothGatt, RxBleConnectionState>> gattAndConnectionStateOutput = new Output<>();
    private final Output<RxBleDeviceServices> servicesDiscoveredOutput = new Output<>();
    private final Output<ByteAssociation<UUID>> readCharacteristicOutput = new Output<>();
    private final Output<ByteAssociation<UUID>> writeCharacteristicOutput = new Output<>();
    private final SerializedRelay<CharacteristicChangedEvent, CharacteristicChangedEvent>
            changedCharacteristicSerializedPublishRelay = PublishRelay.<CharacteristicChangedEvent>create().toSerialized();
    private final Output<ByteAssociation<BluetoothGattDescriptor>> readDescriptorOutput = new Output<>();
    private final Output<ByteAssociation<BluetoothGattDescriptor>> writeDescriptorOutput = new Output<>();
    private final Output<Integer> readRssiOutput = new Output<>();
    private final Output<Integer> changedMtuOutput = new Output<>();
    private final Func1<BleGattException, Object> errorMapper = new Func1<BleGattException, Object>() {
        @Override
        public Object call(BleGattException bleGattException) {
            throw bleGattException;
        }
    };
    private final Observable disconnectedErrorObservable = gattAndConnectionStateOutput.valueRelay
            .filter(new Func1<Pair<BluetoothGatt, RxBleConnectionState>, Boolean>() {
                @Override
                public Boolean call(Pair<BluetoothGatt, RxBleConnectionState> pair) {
                    return isDisconnectedOrDisconnecting(pair);
                }
            })
            .map(new Func1<Pair<BluetoothGatt, RxBleConnectionState>, Object>() {
                @Override
                public Object call(Pair<BluetoothGatt, RxBleConnectionState> bluetoothGattRxBleConnectionStatePair) {
                    throw new BleDisconnectedException(bluetoothGattRxBleConnectionStatePair.first.getDevice().getAddress());
                }
            })
            .mergeWith(gattAndConnectionStateOutput.errorRelay.map(errorMapper))
            .replay()
            .autoConnect(0);

    @Inject
    public RxBleGattCallback(@Named(ClientComponent.NamedSchedulers.GATT_CALLBACK) Scheduler callbackScheduler,
                             BluetoothGattProvider bluetoothGattProvider) {
        this.callbackScheduler = callbackScheduler;
        this.bluetoothGattProvider = bluetoothGattProvider;
    }

    private boolean isDisconnectedOrDisconnecting(Pair<BluetoothGatt, RxBleConnectionState> pair) {
        RxBleConnectionState rxBleConnectionState = pair.second;
        return rxBleConnectionState == RxBleConnectionState.DISCONNECTED || rxBleConnectionState == RxBleConnectionState.DISCONNECTING;
    }

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            RxBleLog.d("onConnectionStateChange newState=%d status=%d", newState, status);
            super.onConnectionStateChange(gatt, status, newState);
            bluetoothGattProvider.updateBluetoothGatt(gatt);

            propagateErrorIfOccurred(gattAndConnectionStateOutput, gatt, status, BleGattOperationType.CONNECTION_STATE);
            gattAndConnectionStateOutput.valueRelay.call(new Pair<>(gatt, mapConnectionStateToRxBleConnectionStatus(newState)));
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            RxBleLog.d("onServicesDiscovered status=%d", status);
            super.onServicesDiscovered(gatt, status);

            if (!propagateErrorIfOccurred(servicesDiscoveredOutput, gatt, status, BleGattOperationType.SERVICE_DISCOVERY)) {
                servicesDiscoveredOutput.valueRelay.call(new RxBleDeviceServices(gatt.getServices()));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            RxBleLog.d("onCharacteristicRead characteristic=%s status=%d", characteristic.getUuid(), status);
            super.onCharacteristicRead(gatt, characteristic, status);

            if (!propagateErrorIfOccurred(
                    readCharacteristicOutput, gatt, characteristic, status, BleGattOperationType.CHARACTERISTIC_READ
            )) {
                readCharacteristicOutput.valueRelay.call(new ByteAssociation<>(characteristic.getUuid(), characteristic.getValue()));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            RxBleLog.d("onCharacteristicWrite characteristic=%s status=%d", characteristic.getUuid(), status);
            super.onCharacteristicWrite(gatt, characteristic, status);

            if (!propagateErrorIfOccurred(
                    writeCharacteristicOutput, gatt, characteristic, status, BleGattOperationType.CHARACTERISTIC_WRITE
            )) {
                writeCharacteristicOutput.valueRelay.call(new ByteAssociation<>(characteristic.getUuid(), characteristic.getValue()));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            RxBleLog.d("onCharacteristicChanged characteristic=%s", characteristic.getUuid());
            super.onCharacteristicChanged(gatt, characteristic);

            /*
             * It is important to call changedCharacteristicSerializedPublishRelay as soon as possible because a quick changing
             * characteristic could lead to out-of-order execution since onCharacteristicChanged may be called on arbitrary
             * threads.
             */
            changedCharacteristicSerializedPublishRelay.call(
                    new CharacteristicChangedEvent(
                            characteristic.getUuid(),
                            characteristic.getInstanceId(),
                            characteristic.getValue()
                    )
            );
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            RxBleLog.d("onCharacteristicRead descriptor=%s status=%d", descriptor.getUuid(), status);
            super.onDescriptorRead(gatt, descriptor, status);

            if (!propagateErrorIfOccurred(readDescriptorOutput, gatt, descriptor, status, BleGattOperationType.DESCRIPTOR_READ)) {
                readDescriptorOutput.valueRelay.call(new ByteAssociation<>(descriptor, descriptor.getValue()));
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            RxBleLog.d("onDescriptorWrite descriptor=%s status=%d", descriptor.getUuid(), status);
            super.onDescriptorWrite(gatt, descriptor, status);

            if (!propagateErrorIfOccurred(writeDescriptorOutput, gatt, descriptor, status, BleGattOperationType.DESCRIPTOR_WRITE)) {
                writeDescriptorOutput.valueRelay.call(new ByteAssociation<>(descriptor, descriptor.getValue()));
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            RxBleLog.d("onReliableWriteCompleted status=%d", status);
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            RxBleLog.d("onReadRemoteRssi rssi=%d status=%d", rssi, status);
            super.onReadRemoteRssi(gatt, rssi, status);

            if (!propagateErrorIfOccurred(readRssiOutput, gatt, status, BleGattOperationType.READ_RSSI)) {
                readRssiOutput.valueRelay.call(rssi);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            RxBleLog.d("onMtuChanged mtu=%d status=%d", mtu, status);
            super.onMtuChanged(gatt, mtu, status);

            if (!propagateErrorIfOccurred(changedMtuOutput, gatt, status, BleGattOperationType.ON_MTU_CHANGED)) {
                changedMtuOutput.valueRelay.call(mtu);
            }
        }
    };

    private RxBleConnectionState mapConnectionStateToRxBleConnectionStatus(int newState) {

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

    private boolean propagateErrorIfOccurred(
            Output output,
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

    private boolean propagateErrorIfOccurred(
            Output output,
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

    private boolean propagateErrorIfOccurred(Output output, BluetoothGatt gatt, int status, BleGattOperationType operationType) {
        return isException(status) && propagateStatusError(output, new BleGattException(gatt, status, operationType));
    }

    private boolean isException(int status) {
        return status != BluetoothGatt.GATT_SUCCESS;
    }

    private boolean propagateStatusError(Output output, BleGattException exception) {
        //noinspection unchecked
        output.errorRelay.call(exception);
        return true;
    }

    private <T> Observable<T> withDisconnectionHandling(Output<T> output) {
        //noinspection unchecked
        return Observable.merge(
                disconnectedErrorObservable,
                gattAndConnectionStateOutput.errorRelay.map(errorMapper),
                output.valueRelay,
                output.errorRelay.map(errorMapper)
        );
    }

    public BluetoothGattCallback getBluetoothGattCallback() {
        return bluetoothGattCallback;
    }

    /**
     * @return Observable that never emits onNexts.
     * @throws BleDisconnectedException emitted in case of a disconnect that is a part of the normal flow
     * @throws BleGattException         emitted in case of connection was interrupted unexpectedly.
     */
    public <T> Observable<T> observeDisconnect() {
        //noinspection unchecked
        return disconnectedErrorObservable;
    }

    /**
     * @return Observable that emits RxBleConnectionState that matches BluetoothGatt's state.
     * Does NOT emit errors even if status != GATT_SUCCESS.
     */
    public Observable<RxBleConnectionState> getOnConnectionStateChange() {
        return gattAndConnectionStateOutput.valueRelay.map(
                new Func1<Pair<BluetoothGatt, RxBleConnectionState>, RxBleConnectionState>() {
                    @Override
                    public RxBleConnectionState call(
                            Pair<BluetoothGatt, RxBleConnectionState> bluetoothGattRxBleConnectionStatePair) {
                        return bluetoothGattRxBleConnectionStatePair.second;
                    }
                }
        ).observeOn(callbackScheduler);
    }

    public Observable<RxBleDeviceServices> getOnServicesDiscovered() {
        return withDisconnectionHandling(servicesDiscoveredOutput).observeOn(callbackScheduler);
    }

    public Observable<Integer> getOnMtuChanged() {
        return withDisconnectionHandling(changedMtuOutput).observeOn(callbackScheduler);
    }

    public Observable<ByteAssociation<UUID>> getOnCharacteristicRead() {
        return withDisconnectionHandling(readCharacteristicOutput).observeOn(callbackScheduler);
    }

    public Observable<ByteAssociation<UUID>> getOnCharacteristicWrite() {
        return withDisconnectionHandling(writeCharacteristicOutput).observeOn(callbackScheduler);
    }

    public Observable<CharacteristicChangedEvent> getOnCharacteristicChanged() {
        //noinspection unchecked
        return Observable.merge(
                disconnectedErrorObservable,
                changedCharacteristicSerializedPublishRelay
        )
                .observeOn(callbackScheduler);
    }

    public Observable<ByteAssociation<BluetoothGattDescriptor>> getOnDescriptorRead() {
        return withDisconnectionHandling(readDescriptorOutput).observeOn(callbackScheduler);
    }

    public Observable<ByteAssociation<BluetoothGattDescriptor>> getOnDescriptorWrite() {
        return withDisconnectionHandling(writeDescriptorOutput).observeOn(callbackScheduler);
    }

    public Observable<Integer> getOnRssiRead() {
        return withDisconnectionHandling(readRssiOutput).observeOn(callbackScheduler);
    }

    private static class Output<T> {
        final PublishRelay<T> valueRelay;
        final PublishRelay<BleGattException> errorRelay;

        Output() {
            this.valueRelay = PublishRelay.create();
            this.errorRelay = PublishRelay.create();
        }
    }
}
