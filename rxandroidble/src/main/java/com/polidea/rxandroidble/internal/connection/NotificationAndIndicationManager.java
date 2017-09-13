package com.polidea.rxandroidble.internal.connection;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.NotificationSetupMode;
import com.polidea.rxandroidble.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble.exceptions.BleConflictingNotificationAlreadySetException;
import com.polidea.rxandroidble.internal.util.ActiveCharacteristicNotification;
import com.polidea.rxandroidble.internal.util.CharacteristicChangedEvent;
import com.polidea.rxandroidble.internal.util.CharacteristicNotificationId;
import com.polidea.rxandroidble.internal.util.ObservableUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Completable;
import rx.Observable;
import rx.functions.Action0;
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
        return Observable.defer(new Func0<Observable<Observable<byte[]>>>() {
            @Override
            public Observable<Observable<byte[]>> call() {
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
                            .doOnUnsubscribe(new Action0() {
                                @Override
                                public void call() {
                                    notificationCompletedSubject.onCompleted();
                                    synchronized (activeNotificationObservableMap) {
                                        activeNotificationObservableMap.remove(id);
                                    }
                                    // teardown the notification
                                    setCharacteristicNotification(bluetoothGatt, characteristic, false)
                                            .compose(setupModeTransformer(descriptorWriter, characteristic, configDisable, setupMode))
                                            .subscribe(
                                                    Actions.empty(),
                                                    Actions.<Throwable>toAction1(Actions.empty())
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
        return Completable.fromAction(new Action0() {
            @Override
            public void call() {
                if (!bluetoothGatt.setCharacteristicNotification(characteristic, isNotificationEnabled)) {
                    throw new BleCannotSetCharacteristicNotificationException(
                            characteristic, BleCannotSetCharacteristicNotificationException.CANNOT_SET_LOCAL_NOTIFICATION, null
                    );
                }
            }
        });
    }

    @NonNull
    private static Completable.Transformer setupModeTransformer(final DescriptorWriter descriptorWriter,
                                                                final BluetoothGattCharacteristic characteristic,
                                                                final byte[] value,
                                                                final NotificationSetupMode mode) {
        return new Completable.Transformer() {
            @Override
            public Completable call(Completable completable) {
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
                .toCompletable()
                .onErrorResumeNext(new Func1<Throwable, Completable>() {
                    @Override
                    public Completable call(Throwable throwable) {
                        return Completable.error(new BleCannotSetCharacteristicNotificationException(
                                bluetoothGattCharacteristic,
                                BleCannotSetCharacteristicNotificationException.CANNOT_WRITE_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR,
                                throwable
                        ));
                    }
                });
    }
}
