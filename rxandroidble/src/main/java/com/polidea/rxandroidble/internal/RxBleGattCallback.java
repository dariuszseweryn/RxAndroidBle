package com.polidea.rxandroidble.internal;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.v4.util.Pair;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.exceptions.BleGattException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func0;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class RxBleGattCallback {

    private Scheduler callbackScheduler = Schedulers.newThread();

    private BehaviorSubject<Void> statusErrorSubject = BehaviorSubject.create();

    private BehaviorSubject<RxBleConnection.RxBleConnectionState> connectionStateBehaviorSubject = BehaviorSubject.create(
            RxBleConnection.RxBleConnectionState.DISCONNECTED);

    private PublishSubject<Map<UUID, Set<UUID>>> servicesDiscoveredPublishSubject = PublishSubject.create();

    private PublishSubject<Pair<UUID, byte[]>> readCharacteristicPublishSubject = PublishSubject.create();

    private PublishSubject<Pair<UUID, byte[]>> writeCharacteristicPublishSubject = PublishSubject.create();

    private PublishSubject<Pair<UUID, byte[]>> changedCharacteristicPublishSubject = PublishSubject.create();

    private PublishSubject<Pair<UUID, byte[]>> reliableWriteCharacteristicPublishSubject = PublishSubject.create();

    private PublishSubject<Pair<UUID, byte[]>> readDescriptorPublishSubject = PublishSubject.create();

    private PublishSubject<Pair<UUID, byte[]>> writeDescriptorPublishSubject = PublishSubject.create();

    private PublishSubject<Integer> readRssiPublishSubject = PublishSubject.create();

    private PublishSubject<Integer> changedMtuPublishSubject = PublishSubject.create();

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (isError(status, BleGattOperationType.CONNECTION_STATE)) {
                return;
            }

            Observable.just(newState)
                    .map(newStateInteger -> {
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
                    })
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(connectionStateBehaviorSubject::onNext);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (isError(status, BleGattOperationType.SERVICE_DISCOVERY)) {
                return;
            }

            Observable.just(gatt)
                    .flatMap(bluetoothGatt -> Observable.from(bluetoothGatt.getServices()))
                    .flatMap(
                            bluetoothGattService -> {
                                final Observable<BluetoothGattCharacteristic> gattCharacteristicObservable =
                                        Observable.from(bluetoothGattService.getCharacteristics());

                                return gattCharacteristicObservable
                                        .map(BluetoothGattCharacteristic::getUuid)
                                        .collect(HashSet::new, HashSet::add);
                            },
                            (Func2<BluetoothGattService, Set<UUID>, Pair<BluetoothGattService, Set<UUID>>>) Pair::new
                    )
                    .collect(
                            (Func0<HashMap<UUID, Set<UUID>>>) HashMap::new,
                            (uuidSetHashMap, bluetoothGattServiceSetPair) -> uuidSetHashMap
                                    .put(bluetoothGattServiceSetPair.first.getUuid(), bluetoothGattServiceSetPair.second)
                    )
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(servicesDiscoveredPublishSubject::onNext);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (isError(status, BleGattOperationType.CHARACTERISTIC_READ)) {
                return;
            }

            Observable.just(characteristic)
                    .map(bluetoothGattCharacteristic -> new Pair<>(bluetoothGattCharacteristic.getUuid(), bluetoothGattCharacteristic.getValue()))
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(readCharacteristicPublishSubject::onNext);

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            if (isError(status, BleGattOperationType.CHARACTERISTIC_WRITE)) {
                return;
            }

            Observable.just(characteristic)
                    .map(bluetoothGattCharacteristic -> new Pair<>(bluetoothGattCharacteristic.getUuid(), bluetoothGattCharacteristic.getValue()))
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(writeCharacteristicPublishSubject::onNext);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            Observable.just(characteristic)
                    .map(bluetoothGattCharacteristic -> new Pair<>(bluetoothGattCharacteristic.getUuid(), bluetoothGattCharacteristic.getValue()))
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(changedCharacteristicPublishSubject::onNext);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);

            if (isError(status, BleGattOperationType.DESCRIPTOR_READ)) {
                return;
            }

            Observable.just(descriptor)
                    .map(gattDescriptor -> new Pair<>(gattDescriptor.getUuid(), gattDescriptor.getValue()))
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(readDescriptorPublishSubject::onNext);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            if (isError(status, BleGattOperationType.DESCRIPTOR_WRITE)) {
                return;
            }

            Observable.just(descriptor)
                    .map(gattDescriptor -> new Pair<>(gattDescriptor.getUuid(), gattDescriptor.getValue()))
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(writeDescriptorPublishSubject::onNext);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);

            if (isError(status, BleGattOperationType.RELIABLE_WRITE_COMPLETED)) {
                return;
            }

            // TODO
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);

            if (isError(status, BleGattOperationType.READ_RSSI)) {
                return;
            }

            Observable.just(rssi)
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(readRssiPublishSubject::onNext);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);

            if (isError(status, BleGattOperationType.ON_MTU_CHANGED)) {
                return;
            }

            Observable.just(mtu)
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(changedMtuPublishSubject::onNext);
        }
    };

    private <T> Observable.Transformer<T, T> getSubscribeAndObserveOnTransformer() {
        return observable -> observable.subscribeOn(callbackScheduler).observeOn(callbackScheduler);
    }

    private boolean isError(int status, BleGattOperationType operationType) {
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

    public Observable<RxBleConnection.RxBleConnectionState> getOnConnectionStateChange() {
        return withHandlingStatusError(connectionStateBehaviorSubject);
    }

    public Observable<Map<UUID, Set<UUID>>> getOnServicesDiscovered() {
        return withHandlingStatusError(servicesDiscoveredPublishSubject.take(1));
    }

    public Observable<Pair<UUID, byte[]>> getOnCharacteristicRead() {
        return withHandlingStatusError(readCharacteristicPublishSubject.take(1));
    }

    public Observable<Pair<UUID, byte[]>> getOnCharacteristicWrite() {
        return withHandlingStatusError(writeCharacteristicPublishSubject.take(1));
    }

    public Observable<Pair<UUID, byte[]>> getOnCharacteristicChanged() {
        return withHandlingStatusError(changedCharacteristicPublishSubject);
    }

    public Observable<Integer> getOnRssiRead() {
        return withHandlingStatusError(readRssiPublishSubject.take(1));
    }
}
