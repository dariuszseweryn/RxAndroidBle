package com.polidea.rxandroidble2.internal.connection;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import androidx.annotation.NonNull;

import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.NotificationSetupMode;
import com.polidea.rxandroidble2.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble2.exceptions.BleConflictingNotificationAlreadySetException;
import com.polidea.rxandroidble2.internal.util.ActiveCharacteristicNotification;
import com.polidea.rxandroidble2.internal.util.CharacteristicNotificationId;
import com.polidea.rxandroidble2.internal.util.ObservableUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

import io.reactivex.Completable;
import io.reactivex.CompletableTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.internal.functions.Functions;
import io.reactivex.subjects.PublishSubject;

@ConnectionScope
class NotificationAndIndicationManager {

    static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    final byte[] configEnableNotification;
    final byte[] configEnableIndication;
    final byte[] configDisable;
    final BluetoothGatt bluetoothGatt;
    final RxBleGattCallback gattCallback;
    final DescriptorWriter descriptorWriter;

    final Map<CharacteristicNotificationId, ActiveCharacteristicNotification> activeNotificationObservableMap = new HashMap<>();

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
        return Observable.defer(() -> {
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
                        .andThen(ObservableUtil.justOnNext(observeOnCharacteristicChangeCallbacks(gattCallback, id)))
                        .compose(setupModeTransformer(descriptorWriter, characteristic, enableNotificationTypeValue, setupMode))
                        .map(observable -> Observable.amb(Arrays.asList(
                                notificationCompletedSubject.cast(byte[].class),
                                observable.takeUntil(notificationCompletedSubject)
                        )))
                        .doFinally(() -> {
                            notificationCompletedSubject.onComplete();
                            synchronized (activeNotificationObservableMap) {
                                activeNotificationObservableMap.remove(id);
                            }
                            // teardown the notification — subscription and result are ignored
                            setCharacteristicNotification(bluetoothGatt, characteristic, false)
                                    .compose(teardownModeTransformer(descriptorWriter, characteristic, configDisable, setupMode))
                                    .subscribe(
                                            Functions.EMPTY_ACTION,
                                            Functions.emptyConsumer()
                                    );
                        })
                        .mergeWith(gattCallback.observeDisconnect())
                        .replay(1)
                        .refCount();
                activeNotificationObservableMap.put(id, new ActiveCharacteristicNotification(newObservable, isIndication));
                return newObservable;
            }
        });
    }

    @NonNull
    static Completable setCharacteristicNotification(final BluetoothGatt bluetoothGatt,
                                                     final BluetoothGattCharacteristic characteristic,
                                                     final boolean isNotificationEnabled) {
        return Completable.fromAction(() -> {
            if (!bluetoothGatt.setCharacteristicNotification(characteristic, isNotificationEnabled)) {
                throw new BleCannotSetCharacteristicNotificationException(
                        characteristic, BleCannotSetCharacteristicNotificationException.CANNOT_SET_LOCAL_NOTIFICATION, null
                );
            }
        });
    }

    @NonNull
    static ObservableTransformer<Observable<byte[]>, Observable<byte[]>> setupModeTransformer(
            final DescriptorWriter descriptorWriter,
            final BluetoothGattCharacteristic characteristic,
            final byte[] value,
            final NotificationSetupMode mode
    ) {
        return upstream -> {
            switch (mode) {

                case COMPAT:
                    return upstream;
                case QUICK_SETUP:
                    final Completable publishedWriteCCCDesc = writeClientCharacteristicConfig(characteristic, descriptorWriter, value)
                            .toObservable()
                            .publish()
                            .autoConnect(2)
                            .ignoreElements();
                    return upstream
                            .mergeWith(publishedWriteCCCDesc)
                            .map(observable -> observable.mergeWith(publishedWriteCCCDesc.onErrorComplete()));
                case DEFAULT:
                default:
                    return writeClientCharacteristicConfig(characteristic, descriptorWriter, value).andThen(upstream);
            }
        };
    }

    @NonNull
    static CompletableTransformer teardownModeTransformer(final DescriptorWriter descriptorWriter,
                                                          final BluetoothGattCharacteristic characteristic,
                                                          final byte[] value,
                                                          final NotificationSetupMode mode) {
        return completable -> {
            if (mode == NotificationSetupMode.COMPAT) {
                return completable;
            } else {
                return completable.andThen(writeClientCharacteristicConfig(characteristic, descriptorWriter, value));
            }
        };
    }

    @NonNull
    static Observable<byte[]> observeOnCharacteristicChangeCallbacks(RxBleGattCallback gattCallback,
                                                                     final CharacteristicNotificationId characteristicId) {
        return gattCallback.getOnCharacteristicChanged()
                .filter(notificationIdWithData -> notificationIdWithData.equals(characteristicId))
                .map(notificationIdWithData -> notificationIdWithData.data);
    }

    @NonNull
    static Completable writeClientCharacteristicConfig(
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
                .onErrorResumeNext(throwable -> Completable.error(new BleCannotSetCharacteristicNotificationException(
                        bluetoothGattCharacteristic,
                        BleCannotSetCharacteristicNotificationException.CANNOT_WRITE_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                        throwable
                )));
    }
}
