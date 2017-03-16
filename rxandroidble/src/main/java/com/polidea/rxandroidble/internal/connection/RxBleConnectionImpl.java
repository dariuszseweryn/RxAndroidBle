package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.os.DeadObjectException;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.NotificationSetupMode;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.RxBleRadioOperationCustom;
import com.polidea.rxandroidble.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble.exceptions.BleConflictingNotificationAlreadySetException;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.operations.OperationsProvider;
import com.polidea.rxandroidble.internal.util.ByteAssociation;
import com.polidea.rxandroidble.internal.util.CharacteristicChangedEvent;
import com.polidea.rxandroidble.internal.util.CharacteristicNotificationId;
import com.polidea.rxandroidble.internal.util.ObservableUtil;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func0;
import rx.functions.Func1;

import static android.bluetooth.BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
import static android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
import static android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
import static rx.Observable.error;
import static rx.Observable.just;

@ConnectionScope
public class RxBleConnectionImpl implements RxBleConnection {

    static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final RxBleRadio rxBleRadio;
    private final RxBleGattCallback gattCallback;
    private final BluetoothGatt bluetoothGatt;
    private final OperationsProvider operationsProvider;
    private final Provider<LongWriteOperationBuilder> longWriteOperationBuilderProvider;
    private final Scheduler callbackScheduler;

    private final AtomicReference<Observable<RxBleDeviceServices>> discoveredServicesCache = new AtomicReference<>();
    private final HashMap<CharacteristicNotificationId, Observable<Observable<byte[]>>> notificationObservableMap = new HashMap<>();
    private final HashMap<CharacteristicNotificationId, Observable<Observable<byte[]>>> indicationObservableMap = new HashMap<>();
    Integer currentMtu = 20; // Default value at the beginning

    @Inject
    public RxBleConnectionImpl(
            RxBleRadio rxBleRadio,
            RxBleGattCallback gattCallback,
            BluetoothGatt bluetoothGatt,
            OperationsProvider operationProvider,
            Provider<LongWriteOperationBuilder> longWriteOperationBuilderProvider,
            @Named(ClientComponent.NamedSchedulers.RADIO_OPERATIONS) Scheduler callbackScheduler
    ) {
        this.rxBleRadio = rxBleRadio;
        this.gattCallback = gattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.operationsProvider = operationProvider;
        this.longWriteOperationBuilderProvider = longWriteOperationBuilderProvider;
        this.callbackScheduler = callbackScheduler;
    }

    @Override
    public LongWriteOperationBuilder createNewLongWriteBuilder() {
        return longWriteOperationBuilderProvider.get();
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Observable<Integer> requestMtu(int mtu) {
        return rxBleRadio
                .queue(operationsProvider.provideMtuChangeOperation(mtu))
                .doOnNext(new Action1<Integer>() {
                    @Override
                    public void call(Integer newMtu) {
                        RxBleConnectionImpl.this.currentMtu = newMtu;
                    }
                });
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
        // TODO: [PU] 16.02.2017 This caching logic potentially could be extracted.
        synchronized (discoveredServicesCache) {
            // checking if there are already cached services
            final Observable<RxBleDeviceServices> sharedObservable = discoveredServicesCache.get();
            if (sharedObservable != null) {
                return sharedObservable;
            }

            final List<BluetoothGattService> services = bluetoothGatt.getServices();
            final Observable<RxBleDeviceServices> newObservable;
            if (services.size() > 0) { // checking if bluetoothGatt has already discovered services (internal cache?)
                newObservable = just(new RxBleDeviceServices(services));
            } else { // performing actual discovery
                newObservable = rxBleRadio
                        .queue(operationsProvider.provideServiceDiscoveryOperation(timeout, timeUnit))
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
        return setupNotification(characteristicUuid, NotificationSetupMode.DEFAULT);
    }

    @Override
    public Observable<Observable<byte[]>> setupNotification(@NonNull BluetoothGattCharacteristic characteristic) {
        return setupNotification(characteristic, NotificationSetupMode.DEFAULT);
    }

    @Override
    public Observable<Observable<byte[]>> setupNotification(@NonNull UUID characteristicUuid, final NotificationSetupMode setupMode) {
        return getCharacteristic(characteristicUuid)
                .flatMap(new Func1<BluetoothGattCharacteristic, Observable<? extends Observable<byte[]>>>() {
                    @Override
                    public Observable<? extends Observable<byte[]>> call(BluetoothGattCharacteristic characteristic) {
                        return setupNotification(characteristic, setupMode);
                    }
                });
    }

    @Override
    public Observable<Observable<byte[]>> setupNotification(@NonNull BluetoothGattCharacteristic characteristic,
                                                            NotificationSetupMode setupMode) {
        return setupServerInitiatedCharacteristicRead(characteristic, setupMode, false);
    }

    @Override
    public Observable<Observable<byte[]>> setupIndication(@NonNull UUID characteristicUuid) {
        return setupIndication(characteristicUuid, NotificationSetupMode.DEFAULT);
    }

    @Override
    public Observable<Observable<byte[]>> setupIndication(@NonNull BluetoothGattCharacteristic characteristic) {
        return setupIndication(characteristic, NotificationSetupMode.DEFAULT);
    }

    @Override
    public Observable<Observable<byte[]>> setupIndication(@NonNull UUID characteristicUuid,
                                                          @NonNull final NotificationSetupMode setupMode) {
        return getCharacteristic(characteristicUuid)
                .flatMap(new Func1<BluetoothGattCharacteristic, Observable<? extends Observable<byte[]>>>() {
                    @Override
                    public Observable<? extends Observable<byte[]>> call(BluetoothGattCharacteristic characteristic) {
                        return setupIndication(characteristic, setupMode);
                    }
                });
    }

    @Override
    public Observable<Observable<byte[]>> setupIndication(@NonNull BluetoothGattCharacteristic characteristic,
                                                          @NonNull NotificationSetupMode setupMode) {
        return setupServerInitiatedCharacteristicRead(characteristic, setupMode, true);
    }

    private synchronized Observable<Observable<byte[]>> setupServerInitiatedCharacteristicRead(
            @NonNull final BluetoothGattCharacteristic characteristic, final NotificationSetupMode setupMode, final boolean withAck
    ) {
        // TODO: [PU] 16.02.2017 Notification & indication setup logic could be extracted.
        return Observable.defer(new Func0<Observable<Observable<byte[]>>>() {
            @Override
            public Observable<Observable<byte[]>> call() {
                synchronized (RxBleConnectionImpl.this) {
                    final CharacteristicNotificationId id
                            = new CharacteristicNotificationId(characteristic.getUuid(), characteristic.getInstanceId());

                    final HashMap<CharacteristicNotificationId, Observable<Observable<byte[]>>> conflictingServerInitiatedReadingMap
                            = withAck ? notificationObservableMap : indicationObservableMap;
                    final boolean conflictingNotificationIsAlreadySet
                            = conflictingServerInitiatedReadingMap.containsKey(id);

                    if (conflictingNotificationIsAlreadySet) {
                        return Observable.error(new BleConflictingNotificationAlreadySetException(characteristic.getUuid(), !withAck));
                    }

                    final HashMap<CharacteristicNotificationId, Observable<Observable<byte[]>>> sameNotificationTypeMap
                            = withAck ? indicationObservableMap : notificationObservableMap;

                    final Observable<Observable<byte[]>> availableObservable = sameNotificationTypeMap.get(id);

                    if (availableObservable != null) {
                        return availableObservable;
                    }

                    final byte[] enableNotificationTypeValue = withAck ? ENABLE_INDICATION_VALUE : ENABLE_NOTIFICATION_VALUE;

                    final Observable<Observable<byte[]>> newObservable = createTriggeredReadObservable(
                            characteristic,
                            enableNotificationTypeValue,
                            setupMode
                    )
                            .doOnUnsubscribe(new Action0() {
                                @Override
                                public void call() {
                                    dismissTriggeredRead(
                                            characteristic, setupMode, id, sameNotificationTypeMap, enableNotificationTypeValue
                                    );
                                }
                            })
                            .map(new Func1<Boolean, Observable<byte[]>>() {
                                @Override
                                public Observable<byte[]> call(Boolean notificationDescriptorData) {
                                    return observeOnCharacteristicChangeCallbacks(id);
                                }
                            })
                            .replay(1)
                            .refCount();
                    sameNotificationTypeMap.put(id, newObservable);
                    return newObservable;
                }
            }
        });
    }

    private Observable<Boolean> createTriggeredReadObservable(final BluetoothGattCharacteristic characteristic, final byte[] enableValue,
                                                              final NotificationSetupMode setupMode) {
        return setCharacteristicNotification(characteristic, true)
                .doOnNext(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean enabledWithSuccess) {
                        if (!enabledWithSuccess) {
                            throw new BleCannotSetCharacteristicNotificationException(characteristic);
                        }
                    }
                })
                .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(Boolean enabledWithSuccess) {
                        return setupCharacteristicDescriptorTriggeredRead(characteristic, setupMode, true, enableValue);
                    }
                })
                .flatMap(new Func1<Boolean, Observable<? extends Boolean>>() {
                    @Override
                    public Observable<? extends Boolean> call(Boolean onNext) {
                        return ObservableUtil.justOnNext(onNext);
                    }
                });
    }

    @NonNull
    private Observable<Boolean> setCharacteristicNotification(final BluetoothGattCharacteristic characteristic,
                                                              final boolean isNotificationEnabled) {
        return Observable.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return bluetoothGatt.setCharacteristicNotification(characteristic, isNotificationEnabled);
            }
        });
    }

    private void dismissTriggeredRead(
            final BluetoothGattCharacteristic characteristic,
            final NotificationSetupMode setupMode,
            final CharacteristicNotificationId characteristicNotificationId,
            HashMap<CharacteristicNotificationId, Observable<Observable<byte[]>>> notificationTypeMap,
            final byte[] enableValue
    ) {
        synchronized (this) {
            notificationTypeMap.remove(characteristicNotificationId);
        }

        setCharacteristicNotification(characteristic, false)
                .flatMap(new Func1<Boolean, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(Boolean descriptor) {
                        return setupCharacteristicDescriptorTriggeredRead(characteristic, setupMode, false, enableValue);
                    }
                })
                .subscribe(
                        Actions.empty(),
                        Actions.<Throwable>toAction1(Actions.empty())
                );
    }

    @NonNull
    private Observable<byte[]> observeOnCharacteristicChangeCallbacks(final CharacteristicNotificationId characteristicId) {
        return gattCallback.getOnCharacteristicChanged()
                .filter(new Func1<CharacteristicChangedEvent, Boolean>() {
                    @Override
                    public Boolean call(CharacteristicChangedEvent notificationIdWithData) {
                        return notificationIdWithData.equals(characteristicId);
                    }
                })
                .map(new Func1<CharacteristicChangedEvent, byte[]>() {
                    @Override
                    public byte[] call(CharacteristicChangedEvent notificationIdWithData) {
                        return notificationIdWithData.data;
                    }
                });
    }

    @NonNull
    private Observable<Boolean> setupCharacteristicDescriptorTriggeredRead(
            final BluetoothGattCharacteristic bluetoothGattCharacteristic, NotificationSetupMode setupMode,
            final boolean enabled, final byte[] enableValue
    ) {
        if (setupMode == NotificationSetupMode.DEFAULT) {
            return getClientCharacteristicConfig(bluetoothGattCharacteristic)
                    .flatMap(new Func1<BluetoothGattDescriptor, Observable<byte[]>>() {
                        @Override
                        public Observable<byte[]> call(BluetoothGattDescriptor bluetoothGattDescriptor) {
                            return writeDescriptor(bluetoothGattDescriptor, enabled ? enableValue : DISABLE_NOTIFICATION_VALUE);
                        }
                    })
                    .onErrorResumeNext(new Func1<Throwable, Observable<byte[]>>() {
                        @Override
                        public Observable<byte[]> call(Throwable throwable) {
                            return error(new BleCannotSetCharacteristicNotificationException(bluetoothGattCharacteristic));
                        }
                    })
                    .switchIfEmpty(Observable.<byte[]>error(
                            new BleCannotSetCharacteristicNotificationException(bluetoothGattCharacteristic)))
                    .map(new Func1<byte[], Boolean>() {
                        @Override
                        public Boolean call(byte[] ignored) {
                            return true;
                        }
                    });
        } else {
            return just(true);
        }

    }

    private Observable<BluetoothGattDescriptor> getClientCharacteristicConfig(final BluetoothGattCharacteristic characteristic) {
        return Observable
                .fromCallable(new Callable<BluetoothGattDescriptor>() {
                    @Override
                    public BluetoothGattDescriptor call() throws Exception {
                        return characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                    }
                })
                .filter(new Func1<BluetoothGattDescriptor, Boolean>() {
                    @Override
                    public Boolean call(BluetoothGattDescriptor bluetoothGattDescriptor) {
                        return bluetoothGattDescriptor != null;
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
        return rxBleRadio.queue(operationsProvider.provideReadCharacteristic(characteristic));
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
        return rxBleRadio.queue(operationsProvider.provideWriteCharacteristic(characteristic, data));
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
        return rxBleRadio
                .queue(operationsProvider.provideReadDescriptor(descriptor))
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
                        return writeDescriptor(bluetoothGattDescriptor, data);
                    }
                });
    }

    @Override
    public Observable<byte[]> writeDescriptor(BluetoothGattDescriptor bluetoothGattDescriptor, byte[] data) {
        return rxBleRadio.queue(operationsProvider.provideWriteDescriptor(bluetoothGattDescriptor, data));
    }

    @Override
    public Observable<Integer> readRssi() {
        return rxBleRadio.queue(operationsProvider.provideRssiReadOperation());
    }

    @Override
    public <T> Observable<T> queue(final RxBleRadioOperationCustom<T> operation) {
        return rxBleRadio.queue(new RxBleRadioOperation<T>() {
            @Override
            @SuppressWarnings("ConstantConditions")
            protected void protectedRun() throws Throwable {
                Observable<T> operationObservable = operation.asObservable(bluetoothGatt, gattCallback, callbackScheduler);
                if (operationObservable == null) {
                    throw new IllegalArgumentException("The custom operation asObservable method must return a non-null observable");
                }

                operationObservable.doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        releaseRadio();
                    }
                }).subscribe(getSubscriber());
            }

            @Override
            protected BleException provideException(DeadObjectException deadObjectException) {
                return new BleDisconnectedException(deadObjectException, bluetoothGatt.getDevice().getAddress());
            }
        });
    }
}
