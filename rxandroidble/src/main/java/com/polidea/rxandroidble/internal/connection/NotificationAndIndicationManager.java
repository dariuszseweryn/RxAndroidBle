package com.polidea.rxandroidble.internal.connection;


import static rx.Observable.just;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.NotificationSetupMode;
import com.polidea.rxandroidble.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble.exceptions.BleConflictingNotificationAlreadySetException;
import com.polidea.rxandroidble.internal.util.CharacteristicChangedEvent;
import com.polidea.rxandroidble.internal.util.CharacteristicNotificationId;
import com.polidea.rxandroidble.internal.util.ObservableUtil;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

@ConnectionScope
class NotificationAndIndicationManager {

    static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final byte[] configEnableNotification;
    private final byte[] configEnableIndication;
    private final byte[] configDisable;
    private final BluetoothGatt bluetoothGatt;
    private final RxBleGattCallback gattCallback;
    private final DescriptorWriter descriptorWriter;

    private final HashMap<CharacteristicNotificationId, Observable<Observable<byte[]>>> notificationObservableMap = new HashMap<>();
    private final HashMap<CharacteristicNotificationId, Observable<Observable<byte[]>>> indicationObservableMap = new HashMap<>();

    @Inject
    NotificationAndIndicationManager(
            @Named(ClientComponent.BluetoothConstants.ENABLE_NOTIFICATION_VALUE) byte[] configEnableNotification,
            @Named(ClientComponent.BluetoothConstants.ENABLE_INDICATION_VALUE) byte[] configEnableIndication,
            @Named(ClientComponent.BluetoothConstants.DISABLE_NOTIFICATION_VALUE) byte[] configDisable,
            BluetoothGatt bluetoothGatt,
            RxBleGattCallback gattCallback,
            DescriptorWriter descriptorWriter
    ) {
        this.configEnableNotification = configEnableNotification;
        this.configEnableIndication = configEnableIndication;
        this.configDisable = configDisable;
        this.bluetoothGatt = bluetoothGatt;
        this.gattCallback = gattCallback;
        this.descriptorWriter = descriptorWriter;
    }

    Observable<Observable<byte[]>> setupServerInitiatedCharacteristicRead(
            @NonNull final BluetoothGattCharacteristic characteristic, final NotificationSetupMode setupMode, final boolean withAck
    ) {
        return Observable.defer(new Func0<Observable<Observable<byte[]>>>() {
            @Override
            public Observable<Observable<byte[]>> call() {
                synchronized (NotificationAndIndicationManager.this) {
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

                    final byte[] enableNotificationTypeValue = withAck ? configEnableIndication : configEnableNotification;
                    final PublishSubject<?> notificationCompletedSubject = PublishSubject.create();

                    final Observable<Observable<byte[]>> newObservable = createTriggeredReadObservable(
                            characteristic,
                            enableNotificationTypeValue,
                            setupMode
                    )
                            .doOnUnsubscribe(new Action0() {
                                @Override
                                public void call() {
                                    notificationCompletedSubject.onCompleted();
                                    dismissTriggeredRead(
                                            characteristic, setupMode, id, sameNotificationTypeMap, enableNotificationTypeValue
                                    );
                                }
                            })
                            .map(new Func1<Boolean, Observable<byte[]>>() {
                                @Override
                                public Observable<byte[]> call(Boolean notificationDescriptorData) {
                                    return observeOnCharacteristicChangeCallbacks(id).takeUntil(notificationCompletedSubject);
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
                            throw new BleCannotSetCharacteristicNotificationException(
                                    characteristic, BleCannotSetCharacteristicNotificationException.CANNOT_SET_LOCAL_NOTIFICATION
                            );
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
                            return descriptorWriter
                                    .writeDescriptor(bluetoothGattDescriptor, enabled ? enableValue : configDisable);
                        }
                    })
                    .onErrorResumeNext(new Func1<Throwable, Observable<byte[]>>() {
                        @Override
                        public Observable<byte[]> call(Throwable throwable) {
                            return Observable.error(new BleCannotSetCharacteristicNotificationException(
                                    bluetoothGattCharacteristic,
                                    BleCannotSetCharacteristicNotificationException.CANNOT_WRITE_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR
                            ));
                        }
                    })
                    .switchIfEmpty(
                            Observable.<byte[]>error(new BleCannotSetCharacteristicNotificationException(
                                    bluetoothGattCharacteristic,
                                    BleCannotSetCharacteristicNotificationException.CANNOT_FIND_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR
                            ))
                    )
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
}
