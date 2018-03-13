package com.polidea.rxandroidble2.mockrxandroidble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;

import com.polidea.rxandroidble2.NotificationSetupMode;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleCustomOperation;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleConflictingNotificationAlreadySetException;
import com.polidea.rxandroidble2.internal.connection.ImmediateSerializedBatchAckStrategy;
import com.polidea.rxandroidble2.internal.util.ObservableUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.functions.Functions;


public class RxBleConnectionMock implements RxBleConnection {

    /**
     * Value used to enable notification for a client configuration descriptor
     */
    private static final byte[] ENABLE_NOTIFICATION_VALUE = {0x01, 0x00};

    /**
     * Value used to enable indication for a client configuration descriptor
     */
    private static final byte[] ENABLE_INDICATION_VALUE = {0x02, 0x00};

    /**
     * Value used to disable notifications or indicatinos
     */
    private static final byte[] DISABLE_NOTIFICATION_VALUE = {0x00, 0x00};

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private HashMap<UUID, Observable<Observable<byte[]>>> notificationObservableMap = new HashMap<>();
    private HashMap<UUID, Observable<Observable<byte[]>>> indicationObservableMap = new HashMap<>();
    private RxBleDeviceServices rxBleDeviceServices;
    private int rssi;
    private int currentMtu = 23;
    private Map<UUID, Observable<byte[]>> characteristicNotificationSources;


    public RxBleConnectionMock(RxBleDeviceServices rxBleDeviceServices,
                               int rssi,
                               Map<UUID, Observable<byte[]>> characteristicNotificationSources) {
        this.rxBleDeviceServices = rxBleDeviceServices;
        this.rssi = rssi;
        this.characteristicNotificationSources = characteristicNotificationSources;
    }

    @Override
    public Completable requestConnectionPriority(int connectionPriority,
                                                 long delay,
                                                 @NonNull TimeUnit timeUnit) {
        return Completable.timer(delay, timeUnit);
    }

    @Override
    public Single<Integer> requestMtu(final int mtu) {
        return Single.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                currentMtu = mtu;
                return mtu;
            }
        });
    }

    @Override
    public int getMtu() {
        return currentMtu;
    }

    @Override
    public Single<RxBleDeviceServices> discoverServices() {
        return Single.just(rxBleDeviceServices);
    }

    @Override
    public Single<RxBleDeviceServices> discoverServices(long timeout, @NonNull TimeUnit timeUnit) {
        return Single.just(rxBleDeviceServices);
    }

    @Override
    public Single<BluetoothGattCharacteristic> getCharacteristic(@NonNull final UUID characteristicUuid) {
        return discoverServices()
                .flatMap(new Function<RxBleDeviceServices, SingleSource<? extends BluetoothGattCharacteristic>>() {
                    @Override
                    public SingleSource<? extends BluetoothGattCharacteristic> apply(RxBleDeviceServices rxBleDeviceServices)
                            throws Exception {
                        return rxBleDeviceServices.getCharacteristic(characteristicUuid);
                    }
                });
    }

    @Override
    public Single<byte[]> readCharacteristic(@NonNull UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid).map(new Function<BluetoothGattCharacteristic, byte[]>() {
            @Override
            public byte[] apply(BluetoothGattCharacteristic bluetoothGattCharacteristic) throws Exception {
                return bluetoothGattCharacteristic.getValue();
            }
        });
    }

    @Override
    public Single<byte[]> readCharacteristic(@NonNull BluetoothGattCharacteristic characteristic) {
        return Single.just(characteristic.getValue());
    }

    @Override
    public Single<byte[]> readDescriptor(@NonNull final UUID serviceUuid, @NonNull final UUID characteristicUuid,
                                             @NonNull final UUID descriptorUuid) {
        return discoverServices()
                .flatMap(new Function<RxBleDeviceServices, SingleSource<BluetoothGattDescriptor>>() {
                    @Override
                    public SingleSource<BluetoothGattDescriptor> apply(RxBleDeviceServices rxBleDeviceServices) {
                        return rxBleDeviceServices.getDescriptor(serviceUuid, characteristicUuid, descriptorUuid);
                    }
                })
                .map(new Function<BluetoothGattDescriptor, byte[]>() {
                    @Override
                    public byte[] apply(BluetoothGattDescriptor bluetoothGattDescriptor) {
                        return bluetoothGattDescriptor.getValue();
                    }
                });
    }

    @Override
    public Single<byte[]> readDescriptor(@NonNull BluetoothGattDescriptor descriptor) {
        return Single.just(descriptor.getValue());
    }

    @Override
    public Single<Integer> readRssi() {
        return Single.just(rssi);
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
    public Observable<Observable<byte[]>> setupNotification(@NonNull final UUID characteristicUuid,
                                                            @NonNull final NotificationSetupMode setupMode) {
        if (indicationObservableMap.containsKey(characteristicUuid)) {
            return Observable.error(new BleConflictingNotificationAlreadySetException(characteristicUuid, true));
        }

        Observable<Observable<byte[]>> availableObservable = notificationObservableMap.get(characteristicUuid);

        if (availableObservable != null) {
            return availableObservable;
        }

        Observable<Observable<byte[]>> newObservable = createCharacteristicNotificationObservable(characteristicUuid, setupMode, false)
                .doFinally(new Action() {
                    @Override
                    public void run() {
                        dismissCharacteristicNotification(characteristicUuid, setupMode, false);
                    }
                })
                .map(new Function<Observable<byte[]>, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> apply(Observable<byte[]> notificationDescriptorData) {
                        return observeOnCharacteristicChangeCallbacks(characteristicUuid);
                    }
                })
                .replay(1)
                .refCount();
        notificationObservableMap.put(characteristicUuid, newObservable);

        return newObservable;
    }

    @Override
    public Observable<Observable<byte[]>> setupNotification(@NonNull BluetoothGattCharacteristic characteristic,
                                                            @NonNull NotificationSetupMode setupMode) {
        return setupNotification(characteristic.getUuid(), setupMode);
    }

    @Override
    public Observable<Observable<byte[]>> setupIndication(@NonNull UUID characteristicUuid) {
        return setupIndication(characteristicUuid, NotificationSetupMode.DEFAULT);
    }

    @Override
    public Observable<Observable<byte[]>> setupIndication(@NonNull BluetoothGattCharacteristic characteristic) {
        return setupIndication(characteristic.getUuid(), NotificationSetupMode.DEFAULT);
    }

    @Override
    public Observable<Observable<byte[]>> setupIndication(@NonNull final UUID characteristicUuid,
                                                          @NonNull final NotificationSetupMode setupMode) {
        if (notificationObservableMap.containsKey(characteristicUuid)) {
            return Observable.error(new BleConflictingNotificationAlreadySetException(characteristicUuid, false));
        }

        Observable<Observable<byte[]>> availableObservable = indicationObservableMap.get(characteristicUuid);

        if (availableObservable != null) {
            return availableObservable;
        }

        Observable<Observable<byte[]>> newObservable = createCharacteristicNotificationObservable(characteristicUuid, setupMode, true)
                .doFinally(new Action() {
                    @Override
                    public void run() {
                        dismissCharacteristicNotification(characteristicUuid, setupMode, true);
                    }
                })
                .map(new Function<Observable<byte[]>, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> apply(Observable<byte[]> notificationDescriptorData) {
                        return observeOnCharacteristicChangeCallbacks(characteristicUuid);
                    }
                })
                .replay(1)
                .refCount();
        indicationObservableMap.put(characteristicUuid, newObservable);

        return newObservable;
    }

    @Override
    public Observable<Observable<byte[]>> setupIndication(@NonNull BluetoothGattCharacteristic characteristic,
                                                          @NonNull NotificationSetupMode setupMode) {
        return setupIndication(characteristic.getUuid(), setupMode);
    }

    @Override
    public Single<byte[]> writeCharacteristic(@NonNull BluetoothGattCharacteristic bluetoothGattCharacteristic, @NonNull byte[] data) {
        bluetoothGattCharacteristic.setValue(data);
        return Single.just(data);
    }

    @Override
    public LongWriteOperationBuilder createNewLongWriteBuilder() {
        return new LongWriteOperationBuilder() {

            private Single<BluetoothGattCharacteristic> bluetoothGattCharacteristicObservable;
            private int maxBatchSize = 20; // default
            private byte[] bytes;
            private WriteOperationAckStrategy writeOperationAckStrategy = // default
                    new ImmediateSerializedBatchAckStrategy();

            @Override
            public LongWriteOperationBuilder setBytes(@NonNull byte[] bytes) {
                this.bytes = bytes;
                return this;
            }

            @Override
            public LongWriteOperationBuilder setCharacteristicUuid(@NonNull final UUID uuid) {
                bluetoothGattCharacteristicObservable = discoverServices().flatMap(
                        new Function<RxBleDeviceServices, SingleSource<BluetoothGattCharacteristic>>() {
                            @Override
                            public SingleSource<BluetoothGattCharacteristic> apply(RxBleDeviceServices rxBleDeviceServices) {
                                return rxBleDeviceServices.getCharacteristic(uuid);
                            }
                        }
                );
                return this;
            }

            @Override
            public LongWriteOperationBuilder setCharacteristic(
                    @NonNull BluetoothGattCharacteristic bluetoothGattCharacteristic) {
                bluetoothGattCharacteristicObservable = Single.just(bluetoothGattCharacteristic);
                return this;
            }

            @Override
            public LongWriteOperationBuilder setMaxBatchSize(int maxBatchSize) {
                this.maxBatchSize = maxBatchSize;
                return this;
            }

            @Override
            public LongWriteOperationBuilder setWriteOperationRetryStrategy(
                    @NonNull WriteOperationRetryStrategy writeOperationRetryStrategy) {
                Log.e("RxBleConnectionMock", "Mock does not support retry strategies. It will always default to no retry.");
                return this;
            }

            @Override
            public LongWriteOperationBuilder setWriteOperationAckStrategy(@NonNull WriteOperationAckStrategy writeOperationAckStrategy) {
                this.writeOperationAckStrategy = writeOperationAckStrategy;
                return this;
            }

            @Override
            public Observable<byte[]> build() {

                if (bluetoothGattCharacteristicObservable == null) {
                    throw new IllegalArgumentException("setCharacteristicUuid() or setCharacteristic() needs to be called before build()");
                }

                if (bytes == null) {
                    throw new IllegalArgumentException("setBytes() needs to be called before build()");
                }

                final boolean excess = bytes.length % maxBatchSize > 0;
                final AtomicInteger numberOfBatches = new AtomicInteger(bytes.length / maxBatchSize + (excess ? 1 : 0));
                return Observable
                        .fromCallable(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return numberOfBatches.get() > 0;
                            }
                        })
                        .compose(writeOperationAckStrategy)
                        .repeatWhen(new Function<Observable<Object>, ObservableSource<?>>() {
                            @Override
                            public ObservableSource<?> apply(Observable<Object> onWriteFinished) throws Exception {
                                return onWriteFinished
                                        .takeWhile(new Predicate<Object>() {
                                            @Override
                                            public boolean test(Object object) {
                                                return numberOfBatches.decrementAndGet() > 0;
                                            }
                                        });
                            }
                        })
                        .ignoreElements()
                        .andThen(Observable.just(bytes));
            }
        };
    }

    @Override
    public Single<byte[]> writeCharacteristic(@NonNull UUID characteristicUuid, @NonNull final byte[] data) {
        return getCharacteristic(characteristicUuid)
                .map(new Function<BluetoothGattCharacteristic, byte[]>() {
                    @Override
                    public byte[] apply(BluetoothGattCharacteristic characteristic) {
                         characteristic.setValue(data);
                         return data;
                    }
                });
    }

    @Override
    public Completable writeDescriptor(@NonNull final UUID serviceUuid, @NonNull final UUID characteristicUuid,
                                              @NonNull final UUID descriptorUuid, @NonNull final byte[] data) {
        return discoverServices()
                .flatMap(new Function<RxBleDeviceServices, SingleSource<BluetoothGattDescriptor>>() {
                    @Override
                    public SingleSource<BluetoothGattDescriptor> apply(RxBleDeviceServices rxBleDeviceServices) {
                        return rxBleDeviceServices.getDescriptor(serviceUuid, characteristicUuid, descriptorUuid);
                    }
                })
                .doOnSuccess(new Consumer<BluetoothGattDescriptor>() {
                    @Override
                    public void accept(BluetoothGattDescriptor bluetoothGattDescriptor) throws Exception {
                        bluetoothGattDescriptor.setValue(data);
                    }
                })
                .toCompletable();
    }

    @Override
    public Completable writeDescriptor(@NonNull final BluetoothGattDescriptor descriptor, @NonNull final byte[] data) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                descriptor.setValue(data);
            }
        });
    }

    private Observable<Observable<byte[]>> createCharacteristicNotificationObservable(final UUID characteristicUuid,
                                                                                      NotificationSetupMode setupMode,
                                                                                      boolean isIndication) {
        return setupCharacteristicNotification(characteristicUuid, setupMode, true, isIndication)
                .andThen(ObservableUtil.justOnNext(true))
                .flatMap(new Function<Boolean, ObservableSource<? extends Observable<byte[]>>>() {
                    @Override
                    public ObservableSource<? extends Observable<byte[]>> apply(Boolean notUsed) {
                        if (!characteristicNotificationSources.containsKey(characteristicUuid)) {
                            return Observable.error(new IllegalStateException("Lack of notification source for given characteristic"));
                        }
                        return Observable.just(characteristicNotificationSources.get(characteristicUuid));
                    }
                });
    }

    private void dismissCharacteristicNotification(UUID characteristicUuid, NotificationSetupMode setupMode, boolean isIndication) {
        notificationObservableMap.remove(characteristicUuid);
        setupCharacteristicNotification(characteristicUuid, setupMode, false, isIndication)
                .subscribe(
                        Functions.EMPTY_ACTION,
                        Functions.emptyConsumer()
                );
    }

    @NonNull
    private Single<BluetoothGattDescriptor> getClientConfigurationDescriptor(UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid)
                .map(new Function<BluetoothGattCharacteristic, BluetoothGattDescriptor>() {
                    @Override
                    public BluetoothGattDescriptor apply(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
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

    @NonNull
    private Completable setupCharacteristicNotification(
            final UUID bluetoothGattCharacteristicUUID,
            final NotificationSetupMode setupMode,
            final boolean enabled,
            final boolean isIndication
    ) {
        if (setupMode == NotificationSetupMode.DEFAULT) {
            final byte[] enableValue = isIndication ? ENABLE_INDICATION_VALUE : ENABLE_NOTIFICATION_VALUE;
            return getClientConfigurationDescriptor(bluetoothGattCharacteristicUUID)
                    .flatMapCompletable(new Function<BluetoothGattDescriptor, Completable>() {
                        @Override
                        public Completable apply(BluetoothGattDescriptor bluetoothGattDescriptor) {
                            return writeDescriptor(bluetoothGattDescriptor, enabled ? enableValue : DISABLE_NOTIFICATION_VALUE);
                        }
                    });
        } else {
            return Completable.complete();
        }

    }

    @Override
    public <T> Observable<T> queue(@NonNull RxBleCustomOperation<T> operation) {
        throw new UnsupportedOperationException("Mock does not support queuing custom operation.");
    }
}
