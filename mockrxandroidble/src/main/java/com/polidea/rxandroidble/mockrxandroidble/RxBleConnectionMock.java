package com.polidea.rxandroidble.mockrxandroidble;

import static android.bluetooth.BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
import static android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
import static android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleConflictingNotificationAlreadySetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Actions;
import rx.functions.Func1;

public class RxBleConnectionMock implements RxBleConnection {

    static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private HashMap<UUID, Observable<Observable<byte[]>>> notificationObservableMap = new HashMap<>();
    private HashMap<UUID, Observable<Observable<byte[]>>> indicationObservableMap = new HashMap<>();
    private RxBleDeviceServices rxBleDeviceServices;
    private int rssi;
    private Map<UUID, Observable<byte[]>> characteristicNotificationSources;


    public RxBleConnectionMock(RxBleDeviceServices rxBleDeviceServices,
                               int rssi,
                               Map<UUID, Observable<byte[]>> characteristicNotificationSources) {
        this.rxBleDeviceServices = rxBleDeviceServices;
        this.rssi = rssi;
        this.characteristicNotificationSources = characteristicNotificationSources;
    }

    @Override
    public Observable<Integer> requestMtu(int mtu) {
        return Observable.just(mtu);
    }

    @Override
    public Observable<RxBleDeviceServices> discoverServices() {
        return Observable.just(rxBleDeviceServices);
    }

    @Override
    public Observable<RxBleDeviceServices> discoverServices(long timeout, TimeUnit timeUnit) {
        return Observable.just(rxBleDeviceServices);
    }

    @Override
    public Observable<BluetoothGattCharacteristic> getCharacteristic(@NonNull final UUID characteristicUuid) {
        return discoverServices()
                .flatMap(new Func1<RxBleDeviceServices, Observable<? extends BluetoothGattCharacteristic>>() {
                    @Override
                    public Observable<? extends BluetoothGattCharacteristic> call(RxBleDeviceServices rxBleDeviceServices) {
                        return rxBleDeviceServices.getCharacteristic(characteristicUuid);
                    }
                });
    }

    @Override
    public Observable<byte[]> readCharacteristic(@NonNull UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid).map(new Func1<BluetoothGattCharacteristic, byte[]>() {
            @Override
            public byte[] call(BluetoothGattCharacteristic characteristic) {
                return characteristic.getValue();
            }
        });
    }

    @Override
    public Observable<byte[]> readCharacteristic(@NonNull BluetoothGattCharacteristic characteristic) {
        return Observable.just(characteristic.getValue());
    }

    @Override
    public Observable<byte[]> readDescriptor(final UUID serviceUuid, final UUID characteristicUuid, final UUID descriptorUuid) {
        return discoverServices()
                .flatMap(new Func1<RxBleDeviceServices, Observable<BluetoothGattDescriptor>>() {
                    @Override
                    public Observable<BluetoothGattDescriptor> call(RxBleDeviceServices rxBleDeviceServices) {
                        return rxBleDeviceServices.getDescriptor(serviceUuid, characteristicUuid, descriptorUuid);
                    }
                })
                .map(new Func1<BluetoothGattDescriptor, byte[]>() {
                    @Override
                    public byte[] call(BluetoothGattDescriptor descriptor) {
                        return descriptor.getValue();
                    }
                });
    }

    @Override
    public Observable<byte[]> readDescriptor(BluetoothGattDescriptor descriptor) {
        return Observable.just(descriptor.getValue());
    }

    @Override
    public Observable<Integer> readRssi() {
        return Observable.just(rssi);
    }

    @Override
    public Observable<Observable<byte[]>> setupNotification(@NonNull final UUID characteristicUuid) {
        if (indicationObservableMap.containsKey(characteristicUuid)) {
            return Observable.error(new BleConflictingNotificationAlreadySetException(characteristicUuid, true));
        }

        Observable<Observable<byte[]>> availableObservable = notificationObservableMap.get(characteristicUuid);

        if (availableObservable != null) {
            return availableObservable;
        }

        Observable<Observable<byte[]>> newObservable = createCharacteristicNotificationObservable(characteristicUuid, false)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        RxBleConnectionMock.this.dismissCharacteristicNotification(characteristicUuid, false);
                    }
                })
                .map(new Func1<Observable<byte[]>, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(Observable<byte[]> notificationDescriptorData) {
                        return RxBleConnectionMock.this.observeOnCharacteristicChangeCallbacks(characteristicUuid);
                    }
                })
                .replay(1)
                .refCount();
        notificationObservableMap.put(characteristicUuid, newObservable);

        return newObservable;
    }

    @Override
    public Observable<Observable<byte[]>> setupNotification(@NonNull BluetoothGattCharacteristic characteristic) {
        return setupNotification(characteristic.getUuid());
    }

    @Override
    public Observable<Observable<byte[]>> setupIndication(@NonNull final UUID characteristicUuid) {
        if (notificationObservableMap.containsKey(characteristicUuid)) {
            return Observable.error(new BleConflictingNotificationAlreadySetException(characteristicUuid, false));
        }

        Observable<Observable<byte[]>> availableObservable = indicationObservableMap.get(characteristicUuid);

        if (availableObservable != null) {
            return availableObservable;
        }

        Observable<Observable<byte[]>> newObservable = createCharacteristicNotificationObservable(characteristicUuid, true)
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        RxBleConnectionMock.this.dismissCharacteristicNotification(characteristicUuid, true);
                    }
                })
                .map(new Func1<Observable<byte[]>, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(Observable<byte[]> notificationDescriptorData) {
                        return RxBleConnectionMock.this.observeOnCharacteristicChangeCallbacks(characteristicUuid);
                    }
                })
                .replay(1)
                .refCount();
        indicationObservableMap.put(characteristicUuid, newObservable);

        return newObservable;
    }

    @Override
    public Observable<Observable<byte[]>> setupIndication(@NonNull BluetoothGattCharacteristic characteristic) {
        return setupIndication(characteristic.getUuid());
    }

    @Override
    public Observable<BluetoothGattCharacteristic> writeCharacteristic(
            @NonNull final BluetoothGattCharacteristic bluetoothGattCharacteristic
    ) {
        return getCharacteristic(bluetoothGattCharacteristic.getUuid())
                .map(new Func1<BluetoothGattCharacteristic, Boolean>() {
                    @Override
                    public Boolean call(BluetoothGattCharacteristic characteristic) {
                        return characteristic.setValue(bluetoothGattCharacteristic.getValue());
                    }
                })
                .flatMap(new Func1<Boolean, Observable<? extends BluetoothGattCharacteristic>>() {
                    @Override
                    public Observable<? extends BluetoothGattCharacteristic> call(Boolean ignored) {
                        return Observable.just(bluetoothGattCharacteristic);
                    }
                });
    }

    @Override
    public Observable<byte[]> writeCharacteristic(@NonNull BluetoothGattCharacteristic bluetoothGattCharacteristic, @NonNull byte[] data) {
        return Observable.just(data);
    }

    @Override
    public Observable<byte[]> writeCharacteristic(@NonNull UUID characteristicUuid, @NonNull final byte[] data) {
        return getCharacteristic(characteristicUuid)
                .map(new Func1<BluetoothGattCharacteristic, Boolean>() {
                    @Override
                    public Boolean call(BluetoothGattCharacteristic characteristic) {
                        return characteristic.setValue(data);
                    }
                })
                .flatMap(new Func1<Boolean, Observable<? extends byte[]>>() {
                    @Override
                    public Observable<? extends byte[]> call(Boolean ignored) {
                        return Observable.just(data);
                    }
                });
    }

    @Override
    public Observable<byte[]> writeDescriptor(
            final UUID serviceUuid, final UUID characteristicUuid, final UUID descriptorUuid, final byte[] data
    ) {
        return discoverServices()
                .flatMap(new Func1<RxBleDeviceServices, Observable<BluetoothGattDescriptor>>() {
                    @Override
                    public Observable<BluetoothGattDescriptor> call(RxBleDeviceServices rxBleDeviceServices) {
                        return rxBleDeviceServices.getDescriptor(serviceUuid, characteristicUuid, descriptorUuid);
                    }
                })
                .map(new Func1<BluetoothGattDescriptor, Boolean>() {
                    @Override
                    public Boolean call(BluetoothGattDescriptor bluetoothGattDescriptor) {
                        return bluetoothGattDescriptor.setValue(data);
                    }
                })
                .flatMap(new Func1<Boolean, Observable<? extends byte[]>>() {
                    @Override
                    public Observable<? extends byte[]> call(Boolean ignored) {
                        return Observable.just(data);
                    }
                });
    }

    @Override
    public Observable<byte[]> writeDescriptor(BluetoothGattDescriptor descriptor, byte[] data) {
        return Observable.just(data);
    }

    private Observable<Observable<byte[]>> createCharacteristicNotificationObservable(
            final UUID characteristicUuid, final boolean isIndication
    ) {
        return getClientConfigurationDescriptor(characteristicUuid)
                .flatMap(new Func1<BluetoothGattDescriptor, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(BluetoothGattDescriptor bluetoothGattDescriptor) {
                        return RxBleConnectionMock.this.setupCharacteristicNotification(bluetoothGattDescriptor, true, isIndication);
                    }
                })
                .flatMap(new Func1<byte[], Observable<? extends Observable<byte[]>>>() {
                    @Override
                    public Observable<? extends Observable<byte[]>> call(byte[] bluetoothGattDescriptorPair) {
                        if (!characteristicNotificationSources.containsKey(characteristicUuid)) {
                            return Observable.error(new IllegalStateException("Lack of notification source for given characteristic"));
                        }
                        return Observable.just(characteristicNotificationSources.get(characteristicUuid));
                    }
                });
    }

    private void dismissCharacteristicNotification(UUID characteristicUuid, final boolean isIndication) {
        notificationObservableMap.remove(characteristicUuid);
        getClientConfigurationDescriptor(characteristicUuid)
                .flatMap(new Func1<BluetoothGattDescriptor, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(BluetoothGattDescriptor descriptor) {
                        return RxBleConnectionMock.this.setupCharacteristicNotification(descriptor, false, isIndication);
                    }
                })
                .subscribe(
                        Actions.empty(),
                        Actions.<Throwable>toAction1(Actions.empty())
                );
    }

    @NonNull
    private Observable<BluetoothGattDescriptor> getClientConfigurationDescriptor(UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid)
                .map(new Func1<BluetoothGattCharacteristic, BluetoothGattDescriptor>() {
                    @Override
                    public BluetoothGattDescriptor call(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
                        BluetoothGattDescriptor bluetoothGattDescriptor =
                                bluetoothGattCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);

                        if (bluetoothGattDescriptor == null) {
                            //adding notification descriptor if not present
                            bluetoothGattDescriptor = new BluetoothGattDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID, 0);
                            bluetoothGattCharacteristic.addDescriptor(bluetoothGattDescriptor);
                        }
                        return bluetoothGattDescriptor;
                    }
                });
    }

    @NonNull
    private Observable<byte[]> observeOnCharacteristicChangeCallbacks(UUID characteristicUuid) {
        return characteristicNotificationSources.get(characteristicUuid);
    }

    private void setCharacteristicNotification(UUID notificationCharacteristicUUID, boolean enable) {
        writeCharacteristic(notificationCharacteristicUUID, new byte[]{(byte) (enable ? 1 : 0)}).subscribe();
    }

    @NonNull
    private Observable<byte[]> setupCharacteristicNotification(
            BluetoothGattDescriptor bluetoothGattDescriptor,
            boolean enabled,
            boolean isIndication
    ) {
        BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattDescriptor.getCharacteristic();
        setCharacteristicNotification(bluetoothGattCharacteristic.getUuid(), enabled);
        final byte[] enableValue = isIndication ? ENABLE_INDICATION_VALUE : ENABLE_NOTIFICATION_VALUE;
        return writeDescriptor(bluetoothGattDescriptor, enabled ? enableValue : DISABLE_NOTIFICATION_VALUE);
    }
}
