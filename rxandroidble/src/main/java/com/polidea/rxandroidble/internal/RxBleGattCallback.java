package com.polidea.rxandroidble.internal;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.v4.util.Pair;
import android.util.Log;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleGattException;
import com.polidea.rxandroidble.exceptions.BleGattOperationType;
import java.util.UUID;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class RxBleGattCallback {
    
    private final String TAG = "RxBleGattCallback(" + System.identityHashCode(this) + ')';

    private Scheduler callbackScheduler = Schedulers.newThread();

    private BehaviorSubject<Void> statusErrorSubject = BehaviorSubject.create();

    private BehaviorSubject<RxBleConnection.RxBleConnectionState> connectionStateBehaviorSubject = BehaviorSubject.create(
            RxBleConnection.RxBleConnectionState.DISCONNECTED);

    private PublishSubject<RxBleDeviceServices> servicesDiscoveredPublishSubject = PublishSubject.create();

    private BehaviorSubject<Pair<UUID, byte[]>> readCharacteristicBehaviorSubject = BehaviorSubject.create();

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
            Log.d(TAG, "onConnectionStateChange");
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
            Log.d(TAG, "onServicesDiscovered");
            super.onServicesDiscovered(gatt, status);

            if (isError(status, BleGattOperationType.SERVICE_DISCOVERY)) {
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
            Log.d(TAG, "onCharacteristicRead");
            super.onCharacteristicRead(gatt, characteristic, status);

            if (isError(status, BleGattOperationType.CHARACTERISTIC_READ)) {
                return;
            }

            Observable.just(characteristic)
                    .map(bluetoothGattCharacteristic -> new Pair<>(bluetoothGattCharacteristic.getUuid(), bluetoothGattCharacteristic.getValue()))
                    .compose(getSubscribeAndObserveOnTransformer())
                    .doOnNext(bytes -> Log.d(TAG, "Read ... " + new String(bytes.second)))
                    .subscribe(readCharacteristicBehaviorSubject::onNext);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead");
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
            Log.d(TAG, "onCharacteristicRead");
            super.onCharacteristicChanged(gatt, characteristic);

            Observable.just(characteristic)
                    .map(bluetoothGattCharacteristic -> new Pair<>(bluetoothGattCharacteristic.getUuid(), bluetoothGattCharacteristic.getValue()))
                    .compose(getSubscribeAndObserveOnTransformer())
                    .subscribe(changedCharacteristicPublishSubject::onNext);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onCharacteristicRead");
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
            Log.d(TAG, "onDescriptorWrite");
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
            Log.d(TAG, "onReliableWriteCompleted");
            super.onReliableWriteCompleted(gatt, status);

            if (isError(status, BleGattOperationType.RELIABLE_WRITE_COMPLETED)) {
                return;
            }

            // TODO
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.d(TAG, "onReadRemoteRssi");
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
            Log.d(TAG, "onMtuChanged");
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

    public Observable<RxBleDeviceServices> getOnServicesDiscovered() {
        return withHandlingStatusError(servicesDiscoveredPublishSubject);
    }

    public Observable<Pair<UUID, byte[]>> getOnCharacteristicRead() {
        Log.d(TAG, "getOnCharacteristicRead");
        return withHandlingStatusError(readCharacteristicBehaviorSubject)
                .doOnSubscribe(() -> Log.d(TAG, "Read . subscribed"))
                .doOnUnsubscribe(() -> {
                    Log.d(TAG, "Read . unsubscribed");
                })
                .doOnError(throwable -> Log.d(TAG, "Read . errored"))
                .doOnCompleted(() -> Log.d(TAG, "Read . completed"))
                .doOnNext(uuidPair -> Log.d(TAG, "Read . onNext"));
    }

    public Observable<Pair<UUID, byte[]>> getOnCharacteristicWrite() {
        return withHandlingStatusError(writeCharacteristicPublishSubject);
    }

    public Observable<Pair<UUID, byte[]>> getOnCharacteristicChanged() {
        return withHandlingStatusError(changedCharacteristicPublishSubject);
    }

    public Observable<Integer> getOnRssiRead() {
        return withHandlingStatusError(readRssiPublishSubject);
    }
}
