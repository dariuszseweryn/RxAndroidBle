package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble.exceptions.BleConflictingNotificationAlreadySetException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationCharacteristicRead;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationCharacteristicWrite;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDescriptorRead;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDescriptorWrite;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationMtuRequest;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationReadRssi;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationServicesDiscover;
import com.polidea.rxandroidble.internal.util.ObservableUtil;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.schedulers.Schedulers;

import static android.bluetooth.BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
import static android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
import static android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
import static rx.Observable.error;
import static rx.Observable.just;

public class RxBleConnectionImpl implements RxBleConnection {

    static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final RxBleRadio rxBleRadio;

    private final RxBleGattCallback gattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final AtomicReference<Observable<RxBleDeviceServices>> discoveredServicesCache = new AtomicReference<>();

    private final HashMap<UUID, Observable<Observable<byte[]>>> notificationObservableMap = new HashMap<>();

    private final HashMap<UUID, Observable<Observable<byte[]>>> indicationObservableMap = new HashMap<>();

    public RxBleConnectionImpl(RxBleRadio rxBleRadio, RxBleGattCallback gattCallback, BluetoothGatt bluetoothGatt) {
        this.rxBleRadio = rxBleRadio;
        this.gattCallback = gattCallback;
        this.bluetoothGatt = bluetoothGatt;
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Observable<Integer> requestMtu(int mtu) {
        return privateRequestMtu(mtu, 10, TimeUnit.SECONDS);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Observable<Integer> privateRequestMtu(int mtu, long timeout, TimeUnit timeUnit) {
        final Observable<Integer> newObservable;
        // performing actual discovery
        newObservable = rxBleRadio
                .queue(new RxBleRadioOperationMtuRequest(
                        mtu,
                        gattCallback,
                        bluetoothGatt,
                        timeout,
                        timeUnit,
                        Schedulers.computation()
                ));

        return newObservable;
    }


    @Override
    public Observable<RxBleDeviceServices> discoverServices() {
        return privateDiscoverServices(20, TimeUnit.SECONDS);
    }

    @Override
    public Observable<RxBleDeviceServices> discoverServices(long timeout, TimeUnit timeUnit) {
        return privateDiscoverServices(timeout, timeUnit);
    }

    private Observable<RxBleDeviceServices> privateDiscoverServices(long timeout, TimeUnit timeUnit) {
        synchronized (discoveredServicesCache) {
            // checking if there are already cached services
            final Observable<RxBleDeviceServices> sharedObservable = this.discoveredServicesCache.get();
            if (sharedObservable != null) {
                return sharedObservable;
            }

            final List<BluetoothGattService> services = bluetoothGatt.getServices();
            final Observable<RxBleDeviceServices> newObservable;
            if (services.size() > 0) { // checking if bluetoothGatt has already discovered services (internal cache?)
                newObservable = just(new RxBleDeviceServices(services));
            } else { // performing actual discovery
                newObservable = rxBleRadio
                        .queue(new RxBleRadioOperationServicesDiscover(
                                gattCallback,
                                bluetoothGatt,
                                timeout,
                                timeUnit,
                                Schedulers.computation()
                        ))
                        .cacheWithInitialCapacity(1);
            }

            discoveredServicesCache.set(newObservable);
            return newObservable;
        }
    }

    @Override
    public Observable<BluetoothGattCharacteristic> getCharacteristic(@NonNull UUID characteristicUuid) {
        return discoverServices()
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic(characteristicUuid));
    }

    @Override
    public Observable<Observable<byte[]>> setupNotification(@NonNull UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid).flatMap(this::setupNotification);
    }

    @Override
    public Observable<Observable<byte[]>> setupNotification(
            @NonNull BluetoothGattCharacteristic characteristic) {
        return setupServerInitiatedCharacteristicRead(characteristic, false);
    }

    @Override
    public Observable<Observable<byte[]>> setupIndication(@NonNull UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid).flatMap(this::setupIndication);
    }

    @Override
    public Observable<Observable<byte[]>> setupIndication(
            @NonNull BluetoothGattCharacteristic characteristic) {
        return setupServerInitiatedCharacteristicRead(characteristic, true);
    }

    private synchronized Observable<Observable<byte[]>> setupServerInitiatedCharacteristicRead(
            @NonNull BluetoothGattCharacteristic characteristic, boolean withAck
    ) {
        return Observable.defer(() -> {
            synchronized (this) {
                final UUID characteristicUuid = characteristic.getUuid();

                final HashMap<UUID, Observable<Observable<byte[]>>> conflictingServerInitiatedReadingMap =
                        withAck ? notificationObservableMap : indicationObservableMap;
                final boolean conflictingNotificationIsAlreadySet = conflictingServerInitiatedReadingMap.containsKey(characteristicUuid);

                if (conflictingNotificationIsAlreadySet) {
                    return Observable.error(new BleConflictingNotificationAlreadySetException(characteristicUuid, !withAck));
                }

                final HashMap<UUID, Observable<Observable<byte[]>>> sameNotificationTypeMap
                        = withAck ? indicationObservableMap : notificationObservableMap;

                final Observable<Observable<byte[]>> availableObservable = sameNotificationTypeMap.get(characteristicUuid);

                if (availableObservable != null) {
                    return availableObservable;
                }

                byte[] enableNotificationTypeValue = withAck ? ENABLE_INDICATION_VALUE : ENABLE_NOTIFICATION_VALUE;

                final Observable<Observable<byte[]>> newObservable = createTriggeredReadObservable(
                        characteristic,
                        enableNotificationTypeValue
                )
                        .doOnUnsubscribe(() -> dismissTriggeredRead(characteristic, sameNotificationTypeMap, enableNotificationTypeValue))
                        .map(notificationDescriptorData -> observeOnCharacteristicChangeCallbacks(characteristicUuid))
                        .replay(1)
                        .refCount();
                sameNotificationTypeMap.put(characteristicUuid, newObservable);
                return newObservable;
            }
        });
    }

    private Observable<byte[]> createTriggeredReadObservable(BluetoothGattCharacteristic characteristic, byte[] enableValue) {
        return getClientCharacteristicConfig(characteristic)
                .flatMap(descriptor -> setupCharacteristicTriggeredRead(descriptor, true, enableValue))
                .flatMap(ObservableUtil::justOnNext);
    }

    private void dismissTriggeredRead(
            BluetoothGattCharacteristic characteristic,
            HashMap<UUID, Observable<Observable<byte[]>>> notificationTypeMap,
            byte[] enableValue
    ) {
        synchronized (this) {
            notificationTypeMap.remove(characteristic.getUuid());
        }

        getClientCharacteristicConfig(characteristic)
                .flatMap(descriptor -> setupCharacteristicTriggeredRead(descriptor, false, enableValue))
                .subscribe(
                        ignored -> {
                        },
                        ignored -> {
                        });
    }

    @NonNull
    private Observable<byte[]> observeOnCharacteristicChangeCallbacks(UUID characteristicUuid) {
        return gattCallback.getOnCharacteristicChanged()
                .filter(uuidPair -> uuidPair.first.equals(characteristicUuid))
                .map(uuidPair -> uuidPair.second);
    }

    @NonNull
    private Observable<byte[]> setupCharacteristicTriggeredRead(
            BluetoothGattDescriptor bluetoothGattDescriptor, boolean enabled, byte[] enableValue
    ) {
        final BluetoothGattCharacteristic characteristic = bluetoothGattDescriptor.getCharacteristic();

        if (bluetoothGatt.setCharacteristicNotification(characteristic, enabled)) {
            return writeDescriptor(bluetoothGattDescriptor, enabled ? enableValue : DISABLE_NOTIFICATION_VALUE)
                    .onErrorResumeNext(throwable -> error(new BleCannotSetCharacteristicNotificationException(characteristic)));
        } else {
            return error(new BleCannotSetCharacteristicNotificationException(characteristic));
        }
    }

    private Observable<BluetoothGattDescriptor> getClientCharacteristicConfig(BluetoothGattCharacteristic characteristic) {
        return Observable.fromCallable(() -> {
            final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);

            if (descriptor == null) {
                throw new BleCannotSetCharacteristicNotificationException(characteristic);
            } else {
                return descriptor;
            }
        });
    }

    @Override
    public Observable<byte[]> readCharacteristic(@NonNull UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid)
                .flatMap(this::readCharacteristic);
    }

    @Override
    public Observable<byte[]> readCharacteristic(
            @NonNull BluetoothGattCharacteristic characteristic) {
        return rxBleRadio.queue(new RxBleRadioOperationCharacteristicRead(
                gattCallback,
                bluetoothGatt,
                characteristic
        ));
    }

    @Override
    public Observable<byte[]> writeCharacteristic(
            @NonNull UUID characteristicUuid, @NonNull byte[] data) {
        return getCharacteristic(characteristicUuid)
                .flatMap(characteristic -> writeCharacteristic(characteristic, data));
    }

    @Deprecated
    @Override
    public Observable<BluetoothGattCharacteristic> writeCharacteristic(
            @NonNull BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        return writeCharacteristic(bluetoothGattCharacteristic, bluetoothGattCharacteristic.getValue())
                .map(bytes -> bluetoothGattCharacteristic);
    }

    @Override
    public Observable<byte[]> writeCharacteristic(
            @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] data) {
        return rxBleRadio.queue(new RxBleRadioOperationCharacteristicWrite(
                gattCallback,
                bluetoothGatt,
                characteristic,
                data));
    }

    @Override
    public Observable<byte[]> readDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid) {
        return discoverServices()
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getDescriptor(serviceUuid, characteristicUuid, descriptorUuid))
                .flatMap(this::readDescriptor);
    }

    @Override
    public Observable<byte[]> readDescriptor(BluetoothGattDescriptor descriptor) {
        return rxBleRadio.queue(
                new RxBleRadioOperationDescriptorRead(gattCallback, bluetoothGatt, descriptor)
        )
                .map(bluetoothGattDescriptorPair -> bluetoothGattDescriptorPair.second);
    }

    @Override
    public Observable<byte[]> writeDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, byte[] data) {
        return discoverServices()
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getDescriptor(serviceUuid, characteristicUuid, descriptorUuid))
                .flatMap(bluetoothGattDescriptor -> writeDescriptor(bluetoothGattDescriptor, data));
    }

    @Override
    public Observable<byte[]> writeDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor, byte[] data) {
        return rxBleRadio.queue(
                new RxBleRadioOperationDescriptorWrite(
                        gattCallback,
                        bluetoothGatt,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                        bluetoothGattDescriptor,
                        data
                )
        );
    }

    @Override
    public Observable<Integer> readRssi() {
        return rxBleRadio.queue(new RxBleRadioOperationReadRssi(gattCallback, bluetoothGatt));
    }
}
