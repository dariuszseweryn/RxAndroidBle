package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble.exceptions.BleCharacteristicNotFoundException;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationCharacteristicRead;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationCharacteristicWrite;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDescriptorRead;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDescriptorWrite;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationReadRssi;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationServicesDiscover;
import com.polidea.rxandroidble.internal.util.ObservableUtil;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;

import static android.bluetooth.BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
import static android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
import static rx.Observable.error;
import static rx.Observable.just;

public class RxBleConnectionImpl implements RxBleConnection {

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final RxBleRadio rxBleRadio;
    private final RxBleGattCallback gattCallback;
    private final BluetoothGatt bluetoothGatt;
    private final AtomicReference<Observable<RxBleDeviceServices>> discoveredServicesCache = new AtomicReference<>();
    private final HashMap<UUID, Observable<Observable<byte[]>>> notificationObservableMap = new HashMap<>();

    public RxBleConnectionImpl(RxBleRadio rxBleRadio, RxBleGattCallback gattCallback, BluetoothGatt bluetoothGatt) {
        this.rxBleRadio = rxBleRadio;
        this.gattCallback = gattCallback;
        this.bluetoothGatt = bluetoothGatt;
    }

    @Override
    public Observable<RxBleConnectionState> getConnectionState() {
        return gattCallback.getOnConnectionStateChange();
    }

    @Override
    public Observable<RxBleDeviceServices> discoverServices() {
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
                newObservable = rxBleRadio.queue(new RxBleRadioOperationServicesDiscover(gattCallback, bluetoothGatt)).cache(1);
            }

            discoveredServicesCache.set(newObservable);
            return newObservable;
        }
    }

    private Observable<BluetoothGattCharacteristic> getCharacteristic(UUID characteristicUuid) {
        return discoverServices()
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic(characteristicUuid));
    }

    @Override
    public Observable<Observable<byte[]>> getNotification(UUID characteristicUuid) {
        synchronized (notificationObservableMap) {
            final Observable<Observable<byte[]>> availableObservable = notificationObservableMap.get(characteristicUuid);

            if (availableObservable != null) {
                return availableObservable;
            }

            final Observable<Observable<byte[]>> newObservable = createCharacteristicNotificationObservable(characteristicUuid)
                    .doOnUnsubscribe(() -> dismissCharacteristicNotification(characteristicUuid))
                    .cache(1)
                    .share();
            notificationObservableMap.put(characteristicUuid, newObservable);
            return newObservable;
        }
    }

    private Observable<Observable<byte[]>> createCharacteristicNotificationObservable(UUID characteristicUuid) {
        return getClientConfigurationDescriptor(characteristicUuid)
                .flatMap(descriptor -> setupCharacteristicNotification(descriptor, true))
                .flatMap(ObservableUtil::justOnNext)
                .map(notificationDescriptorData -> observeOnCharacteristicChangeCallbacks(characteristicUuid));
    }

    @NonNull
    private void dismissCharacteristicNotification(UUID characteristicUuid) {

        synchronized (notificationObservableMap) {
            notificationObservableMap.remove(characteristicUuid);
        }

        getClientConfigurationDescriptor(characteristicUuid)
                .flatMap(descriptor -> setupCharacteristicNotification(descriptor, false))
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
    private Observable<byte[]> setupCharacteristicNotification(BluetoothGattDescriptor bluetoothGattDescriptor, boolean enabled) {
        final BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattDescriptor.getCharacteristic();
        return bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, enabled)
                ? writeDescriptor(bluetoothGattDescriptor, enabled ? ENABLE_NOTIFICATION_VALUE : DISABLE_NOTIFICATION_VALUE)
                : Observable.error(new BleCannotSetCharacteristicNotificationException(bluetoothGattCharacteristic));
    }

    @NonNull
    private Observable<BluetoothGattDescriptor> getClientConfigurationDescriptor(UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid)
                .switchIfEmpty(error(new BleCharacteristicNotFoundException(characteristicUuid)))
                .map(bluetoothGattCharacteristic -> bluetoothGattCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID));
    }

    @Override
    public Observable<byte[]> readCharacteristic(UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid)
                .switchIfEmpty(error(new BleCharacteristicNotFoundException(characteristicUuid)))
                .flatMap(bluetoothGattCharacteristic -> {
                    final RxBleRadioOperationCharacteristicRead operationCharacteristicRead =
                            new RxBleRadioOperationCharacteristicRead(
                                    gattCallback,
                                    bluetoothGatt,
                                    bluetoothGattCharacteristic
                            );
                    return rxBleRadio.queue(operationCharacteristicRead);
                });
    }

    @Override
    public Observable<byte[]> writeCharacteristic(UUID characteristicUuid, byte[] data) {
        return getCharacteristic(characteristicUuid)
                .switchIfEmpty(error(new BleCharacteristicNotFoundException(characteristicUuid)))
                .flatMap(bluetoothGattCharacteristic -> {
                    final RxBleRadioOperationCharacteristicWrite operationCharacteristicWrite = new RxBleRadioOperationCharacteristicWrite(
                            gattCallback,
                            bluetoothGatt,
                            bluetoothGattCharacteristic,
                            data
                    );

                    return rxBleRadio.queue(operationCharacteristicWrite);
                });
    }

    @Override
    public Observable<byte[]> readDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid) {
        return discoverServices()
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getDescriptor(serviceUuid, characteristicUuid, descriptorUuid))
                .flatMap(this::readDescriptor)
                .map(bluetoothGattDescriptorPair -> bluetoothGattDescriptorPair.second);
    }

    private Observable<Pair<BluetoothGattDescriptor, byte[]>> readDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor) {
        return rxBleRadio.queue(
                new RxBleRadioOperationDescriptorRead(gattCallback, bluetoothGatt, bluetoothGattDescriptor)
        );
    }

    @Override
    public Observable<byte[]> writeDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, byte[] data) {
        return discoverServices()
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getDescriptor(serviceUuid, characteristicUuid, descriptorUuid))
                .flatMap(bluetoothGattDescriptor -> writeDescriptor(bluetoothGattDescriptor, data));
    }

    private Observable<byte[]> writeDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor, byte[] data) {
        return rxBleRadio.queue(
                new RxBleRadioOperationDescriptorWrite(gattCallback, bluetoothGatt, bluetoothGattDescriptor, data)
        );
    }

    @Override
    public Observable<Integer> readRssi() {
        return rxBleRadio.queue(new RxBleRadioOperationReadRssi(gattCallback, bluetoothGatt));
    }
}
