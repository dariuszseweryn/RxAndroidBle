package com.polidea.rxandroidble.internal.connection;

import static android.bluetooth.BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
import static android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
import static android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
import static rx.Observable.error;
import static rx.Observable.just;

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
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationCharacteristicRead;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationCharacteristicWrite;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDescriptorRead;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDescriptorWrite;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationMtuRequest;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationReadRssi;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationServicesDiscover;
import com.polidea.rxandroidble.internal.util.ByteAssociation;
import com.polidea.rxandroidble.internal.util.ObservableUtil;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Actions;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class RxBleConnectionImpl implements RxBleConnection {

    static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final RxBleRadio rxBleRadio;

    private final RxBleGattCallback gattCallback;

    private final BluetoothGatt bluetoothGatt;

    private final AtomicReference<Observable<RxBleDeviceServices>> discoveredServicesCache = new AtomicReference<>();

    private final HashMap<Integer, Observable<Observable<byte[]>>> notificationObservableMap = new HashMap<>();

    private final HashMap<Integer, Observable<Observable<byte[]>>> indicationObservableMap = new HashMap<>();

    private final Scheduler timeoutScheduler = Schedulers.computation();

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
                                timeoutScheduler
                        ))
                        .cacheWithInitialCapacity(1);
            }

            discoveredServicesCache.set(newObservable);
            return newObservable;
        }
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
    public Observable<Observable<byte[]>> setupNotification(@NonNull UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid).flatMap(
                new Func1<BluetoothGattCharacteristic, Observable<? extends Observable<byte[]>>>() {
                    @Override
                    public Observable<? extends Observable<byte[]>> call(BluetoothGattCharacteristic characteristic) {
                        return setupNotification(characteristic);
                    }
                });
    }

    @Override
    public Observable<Observable<byte[]>> setupNotification(@NonNull BluetoothGattCharacteristic characteristic) {
        return setupServerInitiatedCharacteristicRead(characteristic, false);
    }

    @Override
    public Observable<Observable<byte[]>> setupIndication(@NonNull UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid).flatMap(
                new Func1<BluetoothGattCharacteristic, Observable<? extends Observable<byte[]>>>() {
                    @Override
                    public Observable<? extends Observable<byte[]>> call(BluetoothGattCharacteristic characteristic) {
                        return setupIndication(characteristic);
                    }
                });
    }

    @Override
    public Observable<Observable<byte[]>> setupIndication(@NonNull BluetoothGattCharacteristic characteristic) {
        return setupServerInitiatedCharacteristicRead(characteristic, true);
    }

    private synchronized Observable<Observable<byte[]>> setupServerInitiatedCharacteristicRead(
            @NonNull final BluetoothGattCharacteristic characteristic, final boolean withAck
    ) {
        return Observable.defer(new Func0<Observable<Observable<byte[]>>>() {
            @Override
            public Observable<Observable<byte[]>> call() {
                synchronized (RxBleConnectionImpl.this) {
                    final int characteristicInstanceId = characteristic.getInstanceId();

                    final HashMap<Integer, Observable<Observable<byte[]>>> conflictingServerInitiatedReadingMap =
                            withAck ? notificationObservableMap : indicationObservableMap;
                    final boolean conflictingNotificationIsAlreadySet =
                            conflictingServerInitiatedReadingMap.containsKey(characteristicInstanceId);

                    if (conflictingNotificationIsAlreadySet) {
                        return Observable.error(new BleConflictingNotificationAlreadySetException(characteristic.getUuid(), !withAck));
                    }

                    final HashMap<Integer, Observable<Observable<byte[]>>> sameNotificationTypeMap
                            = withAck ? indicationObservableMap : notificationObservableMap;

                    final Observable<Observable<byte[]>> availableObservable = sameNotificationTypeMap.get(characteristicInstanceId);

                    if (availableObservable != null) {
                        return availableObservable;
                    }

                    final byte[] enableNotificationTypeValue = withAck ? ENABLE_INDICATION_VALUE : ENABLE_NOTIFICATION_VALUE;

                    final Observable<Observable<byte[]>> newObservable = RxBleConnectionImpl.this.createTriggeredReadObservable(
                            characteristic,
                            enableNotificationTypeValue
                    )
                            .doOnUnsubscribe(new Action0() {
                                @Override
                                public void call() {
                                    RxBleConnectionImpl.this
                                            .dismissTriggeredRead(characteristic, sameNotificationTypeMap, enableNotificationTypeValue);
                                }
                            })
                            .map(new Func1<byte[], Observable<byte[]>>() {
                                @Override
                                public Observable<byte[]> call(byte[] notificationDescriptorData) {
                                    return RxBleConnectionImpl.this.observeOnCharacteristicChangeCallbacks(characteristicInstanceId);
                                }
                            })
                            .replay(1)
                            .refCount();
                    sameNotificationTypeMap.put(characteristicInstanceId, newObservable);
                    return newObservable;
                }
            }
        });
    }

    private Observable<byte[]> createTriggeredReadObservable(BluetoothGattCharacteristic characteristic, final byte[] enableValue) {
        return getClientCharacteristicConfig(characteristic)
                .flatMap(new Func1<BluetoothGattDescriptor, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(BluetoothGattDescriptor descriptor) {
                        return RxBleConnectionImpl.this.setupCharacteristicTriggeredRead(descriptor, true, enableValue);
                    }
                })
                .flatMap(new Func1<byte[], Observable<? extends byte[]>>() {
                    @Override
                    public Observable<? extends byte[]> call(byte[] onNext) {
                        return ObservableUtil.justOnNext(onNext);
                    }
                });
    }

    private void dismissTriggeredRead(
            BluetoothGattCharacteristic characteristic,
            HashMap<Integer, Observable<Observable<byte[]>>> notificationTypeMap,
            final byte[] enableValue
    ) {
        synchronized (this) {
            notificationTypeMap.remove(characteristic.getInstanceId());
        }

        getClientCharacteristicConfig(characteristic)
                .flatMap(new Func1<BluetoothGattDescriptor, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(BluetoothGattDescriptor descriptor) {
                        return RxBleConnectionImpl.this.setupCharacteristicTriggeredRead(descriptor, false, enableValue);
                    }
                })
                .subscribe(
                        Actions.empty(),
                        Actions.<Throwable>toAction1(Actions.empty())
                );
    }

    @NonNull
    private Observable<byte[]> observeOnCharacteristicChangeCallbacks(final Integer characteristicInstanceId) {
        return gattCallback.getOnCharacteristicChanged()
                .filter(new Func1<ByteAssociation<Integer>, Boolean>() {
                    @Override
                    public Boolean call(ByteAssociation<Integer> uuidPair) {
                        return uuidPair.first.equals(characteristicInstanceId);
                    }
                })
                .map(new Func1<ByteAssociation<Integer>, byte[]>() {
                    @Override
                    public byte[] call(ByteAssociation<Integer> uuidPair) {
                        return uuidPair.second;
                    }
                });
    }

    @NonNull
    private Observable<byte[]> setupCharacteristicTriggeredRead(
            BluetoothGattDescriptor bluetoothGattDescriptor, boolean enabled, byte[] enableValue
    ) {
        final BluetoothGattCharacteristic characteristic = bluetoothGattDescriptor.getCharacteristic();

        if (bluetoothGatt.setCharacteristicNotification(characteristic, enabled)) {
            return writeDescriptor(bluetoothGattDescriptor, enabled ? enableValue : DISABLE_NOTIFICATION_VALUE)
                    .onErrorResumeNext(new Func1<Throwable, Observable<? extends byte[]>>() {
                        @Override
                        public Observable<? extends byte[]> call(Throwable throwable) {
                            return error(new BleCannotSetCharacteristicNotificationException(characteristic));
                        }
                    });
        } else {
            return error(new BleCannotSetCharacteristicNotificationException(characteristic));
        }
    }

    private Observable<BluetoothGattDescriptor> getClientCharacteristicConfig(final BluetoothGattCharacteristic characteristic) {
        return Observable.fromCallable(new Callable<BluetoothGattDescriptor>() {
            @Override
            public BluetoothGattDescriptor call() throws Exception {
                final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);

                if (descriptor == null) {
                    throw new BleCannotSetCharacteristicNotificationException(characteristic);
                } else {
                    return descriptor;
                }
            }
        });
    }

    @Override
    public Observable<byte[]> readCharacteristic(@NonNull UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid)
                .flatMap(new Func1<BluetoothGattCharacteristic, Observable<? extends byte[]>>() {
                    @Override
                    public Observable<? extends byte[]> call(BluetoothGattCharacteristic characteristic) {
                        return readCharacteristic(characteristic);
                    }
                });
    }

    @Override
    public Observable<byte[]> readCharacteristic(@NonNull BluetoothGattCharacteristic characteristic) {
        return rxBleRadio.queue(new RxBleRadioOperationCharacteristicRead(
                gattCallback,
                bluetoothGatt,
                characteristic,
                timeoutScheduler));
    }

    @Override
    public Observable<byte[]> writeCharacteristic(@NonNull UUID characteristicUuid, @NonNull final byte[] data) {
        return getCharacteristic(characteristicUuid)
                .flatMap(new Func1<BluetoothGattCharacteristic, Observable<? extends byte[]>>() {
                    @Override
                    public Observable<? extends byte[]> call(BluetoothGattCharacteristic characteristic) {
                        return writeCharacteristic(characteristic, data);
                    }
                });
    }

    @Deprecated
    @Override
    public Observable<BluetoothGattCharacteristic> writeCharacteristic(
            @NonNull final BluetoothGattCharacteristic bluetoothGattCharacteristic
    ) {
        return writeCharacteristic(bluetoothGattCharacteristic, bluetoothGattCharacteristic.getValue())
                .map(new Func1<byte[], BluetoothGattCharacteristic>() {
                    @Override
                    public BluetoothGattCharacteristic call(byte[] bytes) {
                        return bluetoothGattCharacteristic;
                    }
                });
    }

    @Override
    public Observable<byte[]> writeCharacteristic(@NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] data) {
        return rxBleRadio.queue(new RxBleRadioOperationCharacteristicWrite(
                gattCallback,
                bluetoothGatt,
                characteristic,
                data,
                timeoutScheduler));
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
                .flatMap(new Func1<BluetoothGattDescriptor, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(BluetoothGattDescriptor descriptor) {
                        return readDescriptor(descriptor);
                    }
                });
    }

    @Override
    public Observable<byte[]> readDescriptor(BluetoothGattDescriptor descriptor) {
        return rxBleRadio.queue(
                new RxBleRadioOperationDescriptorRead(gattCallback, bluetoothGatt, descriptor, timeoutScheduler)
        )
                .map(new Func1<ByteAssociation<BluetoothGattDescriptor>, byte[]>() {
                    @Override
                    public byte[] call(ByteAssociation<BluetoothGattDescriptor> bluetoothGattDescriptorPair) {
                        return bluetoothGattDescriptorPair.second;
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
                .flatMap(new Func1<BluetoothGattDescriptor, Observable<? extends byte[]>>() {
                    @Override
                    public Observable<? extends byte[]> call(BluetoothGattDescriptor bluetoothGattDescriptor) {
                        return RxBleConnectionImpl.this.writeDescriptor(bluetoothGattDescriptor, data);
                    }
                });
    }

    @Override
    public Observable<byte[]> writeDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor, byte[] data) {
        return rxBleRadio.queue(
                new RxBleRadioOperationDescriptorWrite(
                        gattCallback,
                        bluetoothGatt,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                        bluetoothGattDescriptor,
                        data,
                        timeoutScheduler)
        );
    }

    @Override
    public Observable<Integer> readRssi() {
        return rxBleRadio.queue(new RxBleRadioOperationReadRssi(gattCallback, bluetoothGatt, timeoutScheduler));
    }
}
