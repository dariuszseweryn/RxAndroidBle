package com.polidea.rxandroidble.internal;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleGattException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import java.util.UUID;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class RxBleGattCallback {
    
    private Scheduler callbackScheduler = Schedulers.newThread();

    private BehaviorSubject<Void> statusErrorSubject = BehaviorSubject.create();

    private BehaviorSubject<BluetoothGatt> bluetoothGattBehaviorSubject = BehaviorSubject.create();

    private PublishSubject<RxBleConnection.RxBleConnectionState> connectionStatePublishSubject = PublishSubject.create();

    private PublishSubject<RxBleDeviceServices> servicesDiscoveredPublishSubject = PublishSubject.create();

    private PublishSubject<Pair<UUID, byte[]>> readCharacteristicPublishSubject = PublishSubject.create();

    private PublishSubject<Pair<UUID, byte[]>> writeCharacteristicPublishSubject = PublishSubject.create();

    private PublishSubject<Pair<UUID, byte[]>> changedCharacteristicPublishSubject = PublishSubject.create();

    private PublishSubject<Pair<UUID, byte[]>> reliableWriteCharacteristicPublishSubject = PublishSubject.create();

    private PublishSubject<Pair<BluetoothGattDescriptor, byte[]>> readDescriptorPublishSubject = PublishSubject.create();

    private PublishSubject<Pair<BluetoothGattDescriptor, byte[]>> writeDescriptorPublishSubject = PublishSubject.create();

    private PublishSubject<Integer> readRssiPublishSubject = PublishSubject.create();

    private PublishSubject<Integer> changedMtuPublishSubject = PublishSubject.create();

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
                    .map(mapCharacteristicPairToUuid())
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
                    .map(mapCharacteristicPairToUuid())
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(writeCharacteristicPublishSubject::onNext);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            RxBleLog.d("onCharacteristicChanged characteristic=%s", characteristic.getUuid());
            super.onCharacteristicChanged(gatt, characteristic);

            bluetoothGattBehaviorSubject.onNext(gatt);

            just(characteristic)
                    .map(mapCharacteristicPairToUuid())
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

            // TODO
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
    private Observable<Pair<BluetoothGattCharacteristic, byte[]>> just(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        final byte[] value = bluetoothGattCharacteristic.getValue();
        return Observable.defer(() -> Observable.just(Pair.create(bluetoothGattCharacteristic, value)));
    }

    @NonNull
    private Func1<Pair<BluetoothGattCharacteristic, byte[]>, Pair<UUID, byte[]>> mapCharacteristicPairToUuid() {
        return pair -> new Pair<>(pair.first.getUuid(), pair.second);
    }

    @NonNull
    private Observable<Pair<BluetoothGattDescriptor, byte[]>> just(BluetoothGattDescriptor bluetoothGattDescriptor) {
        final byte[] value = bluetoothGattDescriptor.getValue();
        return Observable.defer(() -> Observable.just(Pair.create(bluetoothGattDescriptor, value)));
    }

    private RxBleConnection.RxBleConnectionState mapConnectionStateToRxBleConnectionStatus(int newState) {

        switch (newState) {
            case BluetoothGatt.STATE_CONNECTING:
                return RxBleConnection.RxBleConnectionState.CONNECTING;
            case BluetoothGatt.STATE_CONNECTED:
                return RxBleConnection.RxBleConnectionState.CONNECTED;
            case BluetoothGatt.STATE_DISCONNECTING:
                return RxBleConnection.RxBleConnectionState.DISCONNECTING;
            default:
                return RxBleConnection.RxBleConnectionState.DISCONNECTED;
        }
    }

    private <T> Observable.Transformer<T, T> getSubscribeAndObserveOnTransformer() {
        return observable -> observable.subscribeOn(callbackScheduler).observeOn(callbackScheduler);
    }

    private boolean propagateStatusErrorIfGattErrorOccurred(int status, BleGattOperationType operationType) {
        final boolean isError = status != BluetoothGatt.GATT_SUCCESS;

        if (isError) {
            statusErrorSubject.onError(new BleGattException(status, operationType));
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

    public Observable<RxBleConnection.RxBleConnectionState> getOnConnectionStateChange() {
        return withHandlingStatusError(connectionStatePublishSubject);
    }

    public Observable<RxBleDeviceServices> getOnServicesDiscovered() {
        return withHandlingStatusError(servicesDiscoveredPublishSubject);
    }

    public Observable<Pair<UUID, byte[]>> getOnCharacteristicRead() {
        return withHandlingStatusError(readCharacteristicPublishSubject);
    }

    public Observable<Pair<UUID, byte[]>> getOnCharacteristicWrite() {
        return withHandlingStatusError(writeCharacteristicPublishSubject);
    }

    public Observable<Pair<UUID, byte[]>> getOnCharacteristicChanged() {
        return withHandlingStatusError(changedCharacteristicPublishSubject);
    }

    public Observable<Pair<BluetoothGattDescriptor, byte[]>> getOnDescriptorRead() {
        return withHandlingStatusError(readDescriptorPublishSubject);
    }

    public Observable<Pair<BluetoothGattDescriptor, byte[]>> getOnDescriptorWrite() {
        return withHandlingStatusError(writeDescriptorPublishSubject);
    }

    public Observable<Integer> getOnRssiRead() {
        return withHandlingStatusError(readRssiPublishSubject);
    }
}
