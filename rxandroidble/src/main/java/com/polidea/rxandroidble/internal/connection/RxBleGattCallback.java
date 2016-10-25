package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleGattException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import com.polidea.rxandroidble.internal.RxBleLog;

import com.polidea.rxandroidble.internal.util.ByteAssociation;
import java.util.UUID;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class RxBleGattCallback {

    public interface Provider {

        RxBleGattCallback provide();
    }

    private final Scheduler callbackScheduler;
    private final BehaviorSubject<Void> statusErrorSubject = BehaviorSubject.create();
    private final BehaviorSubject<BluetoothGatt> bluetoothGattBehaviorSubject = BehaviorSubject.create();
    private final PublishSubject<RxBleConnectionState> connectionStatePublishSubject = PublishSubject.create();
    private final PublishSubject<RxBleDeviceServices> servicesDiscoveredPublishSubject = PublishSubject.create();
    private final PublishSubject<ByteAssociation<UUID>> readCharacteristicPublishSubject = PublishSubject.create();
    private final PublishSubject<ByteAssociation<UUID>> writeCharacteristicPublishSubject = PublishSubject.create();
    private final PublishSubject<ByteAssociation<UUID>> changedCharacteristicPublishSubject = PublishSubject.create();
    private final PublishSubject<ByteAssociation<BluetoothGattDescriptor>> readDescriptorPublishSubject = PublishSubject.create();
    private final PublishSubject<ByteAssociation<BluetoothGattDescriptor>> writeDescriptorPublishSubject = PublishSubject.create();
    private final PublishSubject<Integer> readRssiPublishSubject = PublishSubject.create();
    private final PublishSubject<Integer> changedMtuPublishSubject = PublishSubject.create();
    private final Observable disconnectedErrorObservable = getOnConnectionStateChange()
            .filter(this::isDisconnectedOrDisconnecting)
            .doOnNext(rxBleConnectionState -> bluetoothGattBehaviorSubject.onCompleted())
            .flatMap(rxBleConnectionState -> Observable.error(new BleDisconnectedException()));

    public RxBleGattCallback(Scheduler callbackScheduler) {
        this.callbackScheduler = callbackScheduler;
    }

    private boolean isDisconnectedOrDisconnecting(RxBleConnectionState rxBleConnectionState) {
        return rxBleConnectionState == RxBleConnectionState.DISCONNECTED || rxBleConnectionState == RxBleConnectionState.DISCONNECTING;
    }

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            RxBleLog.d("onConnectionStateChange newState=%d status=%d", newState, status);
            super.onConnectionStateChange(gatt, status, newState);
            bluetoothGattBehaviorSubject.onNext(gatt);

            if (propagateStatusErrorIfGattErrorOccurred(status, BleGattOperationType.CONNECTION_STATE)) {
                return;
            }

            Observable.just(mapConnectionStateToRxBleConnectionStatus(newState))
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(connectionStatePublishSubject::onNext);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            RxBleLog.d("onServicesDiscovered status=%d", status);
            super.onServicesDiscovered(gatt, status);
            bluetoothGattBehaviorSubject.onNext(gatt);

            if (propagateStatusErrorIfGattErrorOccurred(status, BleGattOperationType.SERVICE_DISCOVERY)) {
                return;
            }

            Observable.just(gatt)
                    .map(BluetoothGatt::getServices)
                    .map(RxBleDeviceServices::new)
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(servicesDiscoveredPublishSubject::onNext);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            RxBleLog.d("onCharacteristicRead characteristic=%s status=%d", characteristic.getUuid(), status);
            super.onCharacteristicRead(gatt, characteristic, status);
            bluetoothGattBehaviorSubject.onNext(gatt);

            if (propagateStatusErrorIfGattErrorOccurred(status, BleGattOperationType.CHARACTERISTIC_READ)) {
                return;
            }

            just(characteristic)
                    .map(associateCharacteristicWithBytes())
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(readCharacteristicPublishSubject::onNext);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            RxBleLog.d("onCharacteristicWrite characteristic=%s status=%d", characteristic.getUuid(), status);
            super.onCharacteristicWrite(gatt, characteristic, status);
            bluetoothGattBehaviorSubject.onNext(gatt);

            if (propagateStatusErrorIfGattErrorOccurred(status, BleGattOperationType.CHARACTERISTIC_WRITE)) {
                return;
            }

            just(characteristic)
                    .map(associateCharacteristicWithBytes())
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(writeCharacteristicPublishSubject::onNext);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            RxBleLog.d("onCharacteristicChanged characteristic=%s", characteristic.getUuid());
            super.onCharacteristicChanged(gatt, characteristic);
            bluetoothGattBehaviorSubject.onNext(gatt);

            just(characteristic)
                    .map(associateCharacteristicWithBytes())
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(changedCharacteristicPublishSubject::onNext);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            RxBleLog.d("onCharacteristicRead descriptor=%s status=%d", descriptor.getUuid(), status);
            super.onDescriptorRead(gatt, descriptor, status);
            bluetoothGattBehaviorSubject.onNext(gatt);

            if (propagateStatusErrorIfGattErrorOccurred(status, BleGattOperationType.DESCRIPTOR_READ)) {
                return;
            }

            just(descriptor)
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(readDescriptorPublishSubject::onNext);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            RxBleLog.d("onDescriptorWrite descriptor=%s status=%d", descriptor.getUuid(), status);
            super.onDescriptorWrite(gatt, descriptor, status);
            bluetoothGattBehaviorSubject.onNext(gatt);

            if (propagateStatusErrorIfGattErrorOccurred(status, BleGattOperationType.DESCRIPTOR_WRITE)) {
                return;
            }

            just(descriptor)
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(writeDescriptorPublishSubject::onNext);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            RxBleLog.d("onReliableWriteCompleted status=%d", status);
            super.onReliableWriteCompleted(gatt, status);
            bluetoothGattBehaviorSubject.onNext(gatt);

            if (propagateStatusErrorIfGattErrorOccurred(status, BleGattOperationType.RELIABLE_WRITE_COMPLETED)) {
                return;
            }

            // TODO Implement reliable write
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            RxBleLog.d("onReadRemoteRssi rssi=%d status=%d", rssi, status);
            super.onReadRemoteRssi(gatt, rssi, status);
            bluetoothGattBehaviorSubject.onNext(gatt);

            if (propagateStatusErrorIfGattErrorOccurred(status, BleGattOperationType.READ_RSSI)) {
                return;
            }

            Observable.just(rssi)
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(readRssiPublishSubject::onNext);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            RxBleLog.d("onMtuChanged mtu=%d status=%d", mtu, status);
            super.onMtuChanged(gatt, mtu, status);
            bluetoothGattBehaviorSubject.onNext(gatt);

            if (propagateStatusErrorIfGattErrorOccurred(status, BleGattOperationType.ON_MTU_CHANGED)) {
                return;
            }

            Observable.just(mtu)
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(changedMtuPublishSubject::onNext);
        }
    };

    @NonNull
    private Observable<ByteAssociation<BluetoothGattCharacteristic>> just(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        final byte[] value = bluetoothGattCharacteristic.getValue();
        return Observable.defer(() -> Observable.just(ByteAssociation.create(bluetoothGattCharacteristic, value)));
    }

    @NonNull
    private Func1<ByteAssociation<BluetoothGattCharacteristic>, ByteAssociation<UUID>> associateCharacteristicWithBytes() {
        return pair -> new ByteAssociation<>(pair.first.getUuid(), pair.second);
    }

    @NonNull
    private Observable<ByteAssociation<BluetoothGattDescriptor>> just(BluetoothGattDescriptor bluetoothGattDescriptor) {
        final byte[] value = bluetoothGattDescriptor.getValue();
        return Observable.defer(() -> Observable.just(ByteAssociation.create(bluetoothGattDescriptor, value)));
    }

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

    private <T> Observable.Transformer<T, T> getSubscribeAndObserveOnTransformer() {
        return observable -> observable.subscribeOn(callbackScheduler).observeOn(callbackScheduler);
    }

    private boolean propagateStatusErrorIfGattErrorOccurred(int status, BleGattOperationType operationType) {
        final boolean isError = status != BluetoothGatt.GATT_SUCCESS;

        if (isError) {
            statusErrorSubject.onError(new BleGattException(status, operationType));
            bluetoothGattBehaviorSubject.onCompleted();
        }

        return isError;
    }

    private <T> Observable<T> withHandlingStatusError(Observable<T> observable) {
        //noinspection unchecked
        return Observable.merge(
                (Observable<? extends T>) statusErrorSubject.asObservable(), // statusErrorSubject emits only errors
                observable
        );
    }

    public BluetoothGattCallback getBluetoothGattCallback() {
        return bluetoothGattCallback;
    }

    public Observable<BluetoothGatt> getBluetoothGatt() {
        return bluetoothGattBehaviorSubject;
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

    public Observable<RxBleConnectionState> getOnConnectionStateChange() {
        return withHandlingStatusError(connectionStatePublishSubject);
    }

    public Observable<RxBleDeviceServices> getOnServicesDiscovered() {
        return withHandlingStatusError(servicesDiscoveredPublishSubject);
    }

    public Observable<ByteAssociation<UUID>> getOnCharacteristicRead() {
        return withHandlingStatusError(readCharacteristicPublishSubject);
    }

    public Observable<ByteAssociation<UUID>> getOnCharacteristicWrite() {
        return withHandlingStatusError(writeCharacteristicPublishSubject);
    }

    public Observable<ByteAssociation<UUID>> getOnCharacteristicChanged() {
        return withHandlingStatusError(changedCharacteristicPublishSubject);
    }

    public Observable<ByteAssociation<BluetoothGattDescriptor>> getOnDescriptorRead() {
        return withHandlingStatusError(readDescriptorPublishSubject);
    }

    public Observable<ByteAssociation<BluetoothGattDescriptor>> getOnDescriptorWrite() {
        return withHandlingStatusError(writeDescriptorPublishSubject);
    }

    public Observable<Integer> getOnRssiRead() {
        return withHandlingStatusError(readRssiPublishSubject);
    }
}
