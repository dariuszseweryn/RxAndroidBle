package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Pair;

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
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

@ConnectionScope
public class RxBleGattCallback {

    private final Scheduler callbackScheduler;
    private final BluetoothGattProvider bluetoothGattProvider;
    private final BehaviorSubject statusErrorSubject = BehaviorSubject.create();
    private final PublishSubject<Pair<BluetoothGatt, RxBleConnectionState>> gattAndConnectionStatePublishSubject = PublishSubject.create();
    private final PublishSubject<RxBleDeviceServices> servicesDiscoveredPublishSubject = PublishSubject.create();
    private final PublishSubject<ByteAssociation<UUID>> readCharacteristicPublishSubject = PublishSubject.create();
    private final PublishSubject<ByteAssociation<UUID>> writeCharacteristicPublishSubject = PublishSubject.create();
    private final SerializedSubject<CharacteristicChangedEvent, CharacteristicChangedEvent>
            changedCharacteristicPublishSubject = PublishSubject.<CharacteristicChangedEvent>create().toSerialized();
    private final PublishSubject<ByteAssociation<BluetoothGattDescriptor>> readDescriptorPublishSubject = PublishSubject.create();
    private final PublishSubject<ByteAssociation<BluetoothGattDescriptor>> writeDescriptorPublishSubject = PublishSubject.create();
    private final PublishSubject<Integer> readRssiPublishSubject = PublishSubject.create();
    private final PublishSubject<Integer> changedMtuPublishSubject = PublishSubject.create();
    private final Observable disconnectedErrorObservable = gattAndConnectionStatePublishSubject
            .filter(new Func1<Pair<BluetoothGatt, RxBleConnectionState>, Boolean>() {
                @Override
                public Boolean call(Pair<BluetoothGatt, RxBleConnectionState> pair) {
                    return isDisconnectedOrDisconnecting(pair);
                }
            })
            .flatMap(new Func1<Pair<BluetoothGatt, RxBleConnectionState>, Observable<?>>() {
                @Override
                public Observable<?> call(Pair<BluetoothGatt, RxBleConnectionState> pair) {
                    return Observable.error(new BleDisconnectedException(pair.first.getDevice().getAddress()));
                }
            })
            .doOnTerminate(new Action0() {
                @Override
                public void call() {
                    bluetoothGattProvider.invalidate();
                }
            })
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

            propagateStatusErrorIfGattErrorOccurred(gatt, status, BleGattOperationType.CONNECTION_STATE);
            gattAndConnectionStatePublishSubject.onNext(new Pair<>(gatt, mapConnectionStateToRxBleConnectionStatus(newState)));
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            RxBleLog.d("onServicesDiscovered status=%d", status);
            super.onServicesDiscovered(gatt, status);

            if (!propagateStatusErrorIfGattErrorOccurred(gatt, status, BleGattOperationType.SERVICE_DISCOVERY)) {
                servicesDiscoveredPublishSubject.onNext(new RxBleDeviceServices(gatt.getServices()));
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            RxBleLog.d("onCharacteristicRead characteristic=%s status=%d", characteristic.getUuid(), status);
            super.onCharacteristicRead(gatt, characteristic, status);

            if (!propagateStatusErrorIfGattErrorOccurred(gatt, characteristic, status, BleGattOperationType.CHARACTERISTIC_READ)) {
                readCharacteristicPublishSubject.onNext(new ByteAssociation<>(characteristic.getUuid(), characteristic.getValue()));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            RxBleLog.d("onCharacteristicWrite characteristic=%s status=%d", characteristic.getUuid(), status);
            super.onCharacteristicWrite(gatt, characteristic, status);

            if (!propagateStatusErrorIfGattErrorOccurred(gatt, characteristic, status, BleGattOperationType.CHARACTERISTIC_WRITE)) {
                writeCharacteristicPublishSubject.onNext(new ByteAssociation<>(characteristic.getUuid(), characteristic.getValue()));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            RxBleLog.d("onCharacteristicChanged characteristic=%s", characteristic.getUuid());
            super.onCharacteristicChanged(gatt, characteristic);

            /**
             * It is important to call changedCharacteristicPublishSubject as soon as possible because a quick changing
             * characteristic could lead to out-of-order execution since onCharacteristicChanged may be called on arbitrary
             * threads.
             */
            changedCharacteristicPublishSubject.onNext(
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

            if (!propagateStatusErrorIfGattErrorOccurred(gatt, descriptor, status, BleGattOperationType.DESCRIPTOR_READ)) {
                readDescriptorPublishSubject.onNext(new ByteAssociation<>(descriptor, descriptor.getValue()));
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            RxBleLog.d("onDescriptorWrite descriptor=%s status=%d", descriptor.getUuid(), status);
            super.onDescriptorWrite(gatt, descriptor, status);

            if (!propagateStatusErrorIfGattErrorOccurred(gatt, descriptor, status, BleGattOperationType.DESCRIPTOR_WRITE)) {
                writeDescriptorPublishSubject.onNext(new ByteAssociation<>(descriptor, descriptor.getValue()));
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            RxBleLog.d("onReliableWriteCompleted status=%d", status);
            super.onReliableWriteCompleted(gatt, status);

            if (!propagateStatusErrorIfGattErrorOccurred(gatt, status, BleGattOperationType.RELIABLE_WRITE_COMPLETED)) {
                return; // TODO Implement reliable write
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            RxBleLog.d("onReadRemoteRssi rssi=%d status=%d", rssi, status);
            super.onReadRemoteRssi(gatt, rssi, status);

            if (!propagateStatusErrorIfGattErrorOccurred(gatt, status, BleGattOperationType.READ_RSSI)) {
                readRssiPublishSubject.onNext(rssi);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            RxBleLog.d("onMtuChanged mtu=%d status=%d", mtu, status);
            super.onMtuChanged(gatt, mtu, status);

            if (!propagateStatusErrorIfGattErrorOccurred(gatt, status, BleGattOperationType.ON_MTU_CHANGED)) {
                changedMtuPublishSubject.onNext(mtu);
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

    private boolean propagateStatusErrorIfGattErrorOccurred(
            BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic,
            int status,
            BleGattOperationType operationType
    ) {
        return isException(status) && propagateStatusError(new BleGattCharacteristicException(
                gatt,
                characteristic,
                status,
                operationType
        ));
    }

    private boolean propagateStatusErrorIfGattErrorOccurred(
            BluetoothGatt gatt,
            BluetoothGattDescriptor descriptor,
            int status,
            BleGattOperationType operationType
    ) {
        return isException(status) && propagateStatusError(new BleGattDescriptorException(
                gatt,
                descriptor,
                status,
                operationType
        ));
    }

    private boolean propagateStatusErrorIfGattErrorOccurred(BluetoothGatt gatt, int status, BleGattOperationType operationType) {
        return isException(status) && propagateStatusError(new BleGattException(gatt, status, operationType));
    }

    private boolean isException(int status) {
        return status != BluetoothGatt.GATT_SUCCESS;
    }

    private boolean propagateStatusError(BleGattException exception) {
        statusErrorSubject.onError(exception);
        bluetoothGattProvider.invalidate();
        return true;
    }

    private <T> Observable<T> withHandlingStatusErrorAndDisconnection(Observable<T> observable) {
        //noinspection unchecked
        return Observable.merge(
                statusErrorSubject.asObservable(), // statusErrorSubject emits only errors
                disconnectedErrorObservable,
                observable
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
        return Observable.merge(disconnectedErrorObservable, statusErrorSubject);
    }

    /**
     * @return Observable that emits RxBleConnectionState that matches BluetoothGatt's state.
     * Does NOT emit errors even if status != GATT_SUCCESS.
     */
    public Observable<RxBleConnectionState> getOnConnectionStateChange() {
        return gattAndConnectionStatePublishSubject.map(
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
        return withHandlingStatusErrorAndDisconnection(servicesDiscoveredPublishSubject).observeOn(callbackScheduler);
    }

    public Observable<Integer> getOnMtuChanged() {
        return withHandlingStatusErrorAndDisconnection(changedMtuPublishSubject).observeOn(callbackScheduler);
    }

    public Observable<ByteAssociation<UUID>> getOnCharacteristicRead() {
        return withHandlingStatusErrorAndDisconnection(readCharacteristicPublishSubject).observeOn(callbackScheduler);
    }

    public Observable<ByteAssociation<UUID>> getOnCharacteristicWrite() {
        return withHandlingStatusErrorAndDisconnection(writeCharacteristicPublishSubject).observeOn(callbackScheduler);
    }

    public Observable<CharacteristicChangedEvent> getOnCharacteristicChanged() {
        return withHandlingStatusErrorAndDisconnection(changedCharacteristicPublishSubject).observeOn(callbackScheduler);
    }

    public Observable<ByteAssociation<BluetoothGattDescriptor>> getOnDescriptorRead() {
        return withHandlingStatusErrorAndDisconnection(readDescriptorPublishSubject).observeOn(callbackScheduler);
    }

    public Observable<ByteAssociation<BluetoothGattDescriptor>> getOnDescriptorWrite() {
        return withHandlingStatusErrorAndDisconnection(writeDescriptorPublishSubject).observeOn(callbackScheduler);
    }

    public Observable<Integer> getOnRssiRead() {
        return withHandlingStatusErrorAndDisconnection(readRssiPublishSubject).observeOn(callbackScheduler);
    }
}
