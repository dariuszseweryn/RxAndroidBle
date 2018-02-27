package com.polidea.rxandroidble2.internal.connection;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.NotificationSetupMode;
import com.polidea.rxandroidble2.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble2.exceptions.BleConflictingNotificationAlreadySetException;
import com.polidea.rxandroidble2.internal.util.ActiveCharacteristicNotification;
import com.polidea.rxandroidble2.internal.util.CharacteristicChangedEvent;
import com.polidea.rxandroidble2.internal.util.CharacteristicNotificationId;
import com.polidea.rxandroidble2.internal.util.ObservableUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

import io.reactivex.Completable;
import io.reactivex.CompletableSource;
import io.reactivex.CompletableTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.functions.Functions;
import io.reactivex.subjects.PublishSubject;

@ConnectionScope
class NotificationAndIndicationManager {

    static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final byte[] configEnableNotification;
    private final byte[] configEnableIndication;
    private final byte[] configDisable;
    private final BluetoothGatt bluetoothGatt;
    private final RxBleGattCallback gattCallback;
    private final DescriptorWriter descriptorWriter;

    private final Map<CharacteristicNotificationId, ActiveCharacteristicNotification> activeNotificationObservableMap = new HashMap<>();

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
            @NonNull final BluetoothGattCharacteristic characteristic, final NotificationSetupMode setupMode, final boolean isIndication
    ) {
        return Observable.defer(new Callable<ObservableSource<Observable<byte[]>>>() {
            @Override
            public ObservableSource<Observable<byte[]>> call() throws Exception {
                synchronized (activeNotificationObservableMap) {
                    final CharacteristicNotificationId id
                            = new CharacteristicNotificationId(characteristic.getUuid(), characteristic.getInstanceId());

                    final ActiveCharacteristicNotification activeCharacteristicNotification = activeNotificationObservableMap.get(id);

                    if (activeCharacteristicNotification != null) {
                        if (activeCharacteristicNotification.isIndication == isIndication) {
                            return activeCharacteristicNotification.notificationObservable;
                        } else {
                            return Observable.error(
                                    new BleConflictingNotificationAlreadySetException(characteristic.getUuid(), !isIndication)
                            );
                        }
                    }

                    final byte[] enableNotificationTypeValue = isIndication ? configEnableIndication : configEnableNotification;
                    final PublishSubject<?> notificationCompletedSubject = PublishSubject.create();

                    final Observable<Observable<byte[]>> newObservable = setCharacteristicNotification(bluetoothGatt, characteristic, true)
                            .compose(setupModeTransformer(descriptorWriter, characteristic, enableNotificationTypeValue, setupMode))
                            .andThen(ObservableUtil.justOnNext(
                                    observeOnCharacteristicChangeCallbacks(gattCallback, id).takeUntil(notificationCompletedSubject)
                            ))
                            .doFinally(new Action() {
                                @Override
                                public void run() {
                                    notificationCompletedSubject.onComplete();
                                    synchronized (activeNotificationObservableMap) {
                                        activeNotificationObservableMap.remove(id);
                                    }
                                    // teardown the notification
                                    setCharacteristicNotification(bluetoothGatt, characteristic, false)
                                            .compose(setupModeTransformer(descriptorWriter, characteristic, configDisable, setupMode))
                                            .subscribe(
                                                    Functions.EMPTY_ACTION,
                                                    Functions.emptyConsumer()
                                            );
                                }
                            })
                            .mergeWith(gattCallback.<Observable<byte[]>>observeDisconnect())
                            .replay(1)
                            .refCount();
                    activeNotificationObservableMap.put(id, new ActiveCharacteristicNotification(newObservable, isIndication));
                    return newObservable;
                }
            }
        });
    }

    @NonNull
    private static Completable setCharacteristicNotification(final BluetoothGatt bluetoothGatt,
                                                             final BluetoothGattCharacteristic characteristic,
                                                             final boolean isNotificationEnabled) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() {
                if (!bluetoothGatt.setCharacteristicNotification(characteristic, isNotificationEnabled)) {
                    throw new BleCannotSetCharacteristicNotificationException(
                            characteristic, BleCannotSetCharacteristicNotificationException.CANNOT_SET_LOCAL_NOTIFICATION, null
                    );
                }
            }
        });
    }

    @NonNull
    private static CompletableTransformer setupModeTransformer(final DescriptorWriter descriptorWriter,
                                                               final BluetoothGattCharacteristic characteristic,
                                                               final byte[] value,
                                                               final NotificationSetupMode mode) {
        return new CompletableTransformer() {
            @Override
            public Completable apply(Completable completable) {
                if (mode == NotificationSetupMode.DEFAULT) {
                    return completable.andThen(writeClientCharacteristicConfig(characteristic, descriptorWriter, value));
                } else { // NotificationSetupMode.COMPAT
                    return completable;
                }
            }
        };
    }

    @NonNull
    private static Observable<byte[]> observeOnCharacteristicChangeCallbacks(RxBleGattCallback gattCallback,
                                                                             final CharacteristicNotificationId characteristicId) {
        return gattCallback.getOnCharacteristicChanged()
                .filter(new Predicate<CharacteristicChangedEvent>() {
                    @Override
                    public boolean test(CharacteristicChangedEvent notificationIdWithData) {
                        return notificationIdWithData.equals(characteristicId);
                    }
                })
                .map(new Function<CharacteristicChangedEvent, byte[]>() {
                    @Override
                    public byte[] apply(CharacteristicChangedEvent notificationIdWithData) {
                        return notificationIdWithData.data;
                    }
                });
    }

    @NonNull
    private static Completable writeClientCharacteristicConfig(
            final BluetoothGattCharacteristic bluetoothGattCharacteristic,
            final DescriptorWriter descriptorWriter,
            final byte[] value
    ) {
        final BluetoothGattDescriptor descriptor = bluetoothGattCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
        if (descriptor == null) {
            return Completable.error(new BleCannotSetCharacteristicNotificationException(
                    bluetoothGattCharacteristic,
                    BleCannotSetCharacteristicNotificationException.CANNOT_FIND_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                    null
            ));
        }

        return descriptorWriter.writeDescriptor(descriptor, value)
                .onErrorResumeNext(new Function<Throwable, CompletableSource>() {
                    @Override
                    public CompletableSource apply(Throwable throwable) throws Exception {
                        return Completable.error(new BleCannotSetCharacteristicNotificationException(
                                bluetoothGattCharacteristic,
                                BleCannotSetCharacteristicNotificationException.CANNOT_WRITE_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                                throwable
                        ));
                    }
                });
    }
}
