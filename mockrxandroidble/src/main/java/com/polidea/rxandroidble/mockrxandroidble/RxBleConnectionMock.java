package com.polidea.rxandroidble.mockrxandroidble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDeviceServices;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import rx.Observable;
import rx.Subscriber;
import rx.subjects.BehaviorSubject;

class RxBleConnectionMock implements RxBleConnection {

    private final HashMap<UUID, Observable<Observable<byte[]>>> notificationObservableMap = new HashMap<>();
    private RxBleDeviceServices rxBleDeviceServices;
    private Integer rssid;
    private Map<UUID, Observable<byte[]>> characteristicNotificationSources;
    private BehaviorSubject<RxBleConnectionState> connectionStatePublishSubject;


    public RxBleConnectionMock(RxBleDeviceServices rxBleDeviceServices, Integer rssid, Map<UUID, Observable<byte[]>> characteristicNotificationSources, BehaviorSubject<RxBleConnectionState> connectionStatePublishSubject) {
        this.rxBleDeviceServices = rxBleDeviceServices;
        this.rssid = rssid;
        this.characteristicNotificationSources = characteristicNotificationSources;
        this.connectionStatePublishSubject = connectionStatePublishSubject;
    }

    @Override
    public Observable<RxBleDeviceServices> discoverServices() {
        return Observable.just(rxBleDeviceServices);
    }

    @Override
    public Observable<RxBleConnectionState> getConnectionState() {
        return connectionStatePublishSubject;
    }

    @Override
    public Observable<Observable<byte[]>> getNotification(UUID characteristicUuid) {
        synchronized (notificationObservableMap) {
            final Observable<Observable<byte[]>> availableObservable = notificationObservableMap.get(characteristicUuid);

            if (availableObservable != null) {
                return availableObservable;
            }

            final Observable<Observable<byte[]>> newObservable = createCharacteristicNotificationObservable(characteristicUuid);
            notificationObservableMap.put(characteristicUuid, newObservable);

            return newObservable;
        }
    }

    @Override
    public Observable<byte[]> readCharacteristic(UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid).map(BluetoothGattCharacteristic::getValue);
    }

    @Override
    public Observable<byte[]> readDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid) {
        return discoverServices()
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getDescriptor(serviceUuid, characteristicUuid, descriptorUuid))
                .map(BluetoothGattDescriptor::getValue);
    }

    @Override
    public Observable<Integer> readRssi() {
        return Observable.just(rssid);
    }

    @Override
    public Observable<byte[]> writeCharacteristic(UUID characteristicUuid, byte[] data) {
        return getCharacteristic(characteristicUuid).map(characteristic -> characteristic.setValue(data)).flatMap(ignored -> Observable.just(data));
    }

    @Override
    public Observable<byte[]> writeDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, byte[] data) {
        return discoverServices()
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getDescriptor(serviceUuid, characteristicUuid, descriptorUuid))
                .map(bluetoothGattDescriptor -> bluetoothGattDescriptor.setValue(data)).flatMap(ignored -> Observable.just(data));
    }

    private Observable<Observable<byte[]>> createCharacteristicNotificationObservable(UUID characteristicUuid) {
        return getClientConfigurationDescriptor(characteristicUuid)
                .flatMap(bluetoothGattDescriptor -> {
                    final BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattDescriptor.getCharacteristic();
                    setCharacteristicNotification(bluetoothGattCharacteristic.getUuid(), true);
                    return writeDescriptor(bluetoothGattDescriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                })
                .flatMap(bluetoothGattDescriptorPair -> Observable.create(subscriber -> subscriber.onNext(bluetoothGattDescriptorPair)))
                .flatMap(bluetoothGattDescriptorPair -> {
                    if (!characteristicNotificationSources.containsKey(characteristicUuid)) {
                        return Observable.error(new IllegalStateException("Lack of notification source for given characteristic"));
                    }
                    return Observable.just(characteristicNotificationSources.get(characteristicUuid));
                })
                .doOnUnsubscribe(() -> {
                            synchronized (notificationObservableMap) {
                                notificationObservableMap.remove(characteristicUuid);
                            }
                            getClientConfigurationDescriptor(characteristicUuid)
                                    .flatMap(bluetoothGattDescriptor -> {
                                        setCharacteristicNotification(bluetoothGattDescriptor.getCharacteristic().getUuid(), false);
                                        return writeDescriptor(bluetoothGattDescriptor, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                                    })
                                    .subscribe(
                                            ignored -> {
                                            },
                                            ignored -> {
                                            }
                                    );
                        }
                )
                .cache(1)
                .share();
    }

    private Observable<BluetoothGattCharacteristic> getCharacteristic(UUID characteristicUuid) {
        return discoverServices()
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic(characteristicUuid));
    }

    @NonNull
    private Observable<BluetoothGattDescriptor> getClientConfigurationDescriptor(UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid)
                .map(bluetoothGattCharacteristic -> {
                    BluetoothGattDescriptor bluetoothGattDescriptor = bluetoothGattCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));

                    if (bluetoothGattDescriptor == null) {
                        //adding notification descriptor if not present
                        bluetoothGattDescriptor = new BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"), 0);
                        bluetoothGattCharacteristic.addDescriptor(bluetoothGattDescriptor);
                    }
                    return bluetoothGattDescriptor;
                });
    }

    private void setCharacteristicNotification(UUID notificationCharacteristicUUID, boolean enable) {
        writeCharacteristic(notificationCharacteristicUUID, new byte[]{(byte) (enable ? 1 : 0)}).subscribe();
    }

    private Observable<Pair<BluetoothGattDescriptor, byte[]>> writeDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor, byte[] data) {
        return Observable.create(new Observable.OnSubscribe<Pair<BluetoothGattDescriptor, byte[]>>() {
            @Override
            public void call(Subscriber<? super Pair<BluetoothGattDescriptor, byte[]>> subscriber) {
                subscriber.onNext(new Pair<>(bluetoothGattDescriptor, data));
                subscriber.onCompleted();
            }
        });
    }
}
