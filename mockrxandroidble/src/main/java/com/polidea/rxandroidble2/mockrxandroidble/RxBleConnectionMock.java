package com.polidea.rxandroidble2.mockrxandroidble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import androidx.annotation.NonNull;

import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.polidea.rxandroidble2.ConnectionParameters;
import com.polidea.rxandroidble2.NotificationSetupMode;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleCustomOperation;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleConflictingNotificationAlreadySetException;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.exceptions.BleGattCharacteristicException;
import com.polidea.rxandroidble2.exceptions.BleGattDescriptorException;
import com.polidea.rxandroidble2.exceptions.BleGattOperationType;
import com.polidea.rxandroidble2.internal.Priority;
import com.polidea.rxandroidble2.internal.connection.ImmediateSerializedBatchAckStrategy;
import com.polidea.rxandroidble2.internal.util.ObservableUtil;
import com.polidea.rxandroidble2.mockrxandroidble.callbacks.results.RxBleGattReadResultMock;
import com.polidea.rxandroidble2.mockrxandroidble.callbacks.results.RxBleGattWriteResultMock;
import com.polidea.rxandroidble2.mockrxandroidble.callbacks.RxBleCharacteristicReadCallback;
import com.polidea.rxandroidble2.mockrxandroidble.callbacks.RxBleCharacteristicWriteCallback;
import com.polidea.rxandroidble2.mockrxandroidble.callbacks.RxBleDescriptorReadCallback;
import com.polidea.rxandroidble2.mockrxandroidble.callbacks.RxBleDescriptorWriteCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Predicate;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.subjects.CompletableSubject;
import io.reactivex.subjects.SingleSubject;

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
    private Map<UUID, RxBleCharacteristicReadCallback> characteristicReadCallbacks;
    private Map<UUID, RxBleCharacteristicWriteCallback> characteristicWriteCallbacks;
    private Map<UUID, Map<UUID, RxBleDescriptorReadCallback>> descriptorReadCallbacks;
    private Map<UUID, Map<UUID, RxBleDescriptorWriteCallback>> descriptorWriteCallbacks;
    private RxBleDeviceMock deviceMock;

    public RxBleConnectionMock(RxBleDeviceServices rxBleDeviceServices,
                               int rssi,
                               Map<UUID, Observable<byte[]>> characteristicNotificationSources,
                               Map<UUID, RxBleCharacteristicReadCallback> characteristicReadCallbacks,
                               Map<UUID, RxBleCharacteristicWriteCallback> characteristicWriteCallbacks,
                               Map<UUID, Map<UUID, RxBleDescriptorReadCallback>> descriptorReadCallbacks,
                               Map<UUID, Map<UUID, RxBleDescriptorWriteCallback>> descriptorWriteCallbacks) {
        this.rxBleDeviceServices = rxBleDeviceServices;
        this.rssi = rssi;
        this.characteristicNotificationSources = characteristicNotificationSources;
        this.characteristicReadCallbacks = characteristicReadCallbacks;
        this.characteristicWriteCallbacks = characteristicWriteCallbacks;
        this.descriptorReadCallbacks = descriptorReadCallbacks;
        this.descriptorWriteCallbacks = descriptorWriteCallbacks;
    }

    void setDeviceMock(RxBleDeviceMock deviceMock) {
        this.deviceMock = deviceMock;
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

    public int getRssi() {
        return rssi;
    }

    RxBleDeviceServices getRxBleDeviceServices() {
        return rxBleDeviceServices;
    }

    List<BluetoothGattService> getBluetoothGattServices() {
        return rxBleDeviceServices.getBluetoothGattServices();
    }

    List<UUID> getServiceUuids() {
        List<UUID> uuids = new ArrayList<>();
        for (BluetoothGattService service : getBluetoothGattServices()) {
            uuids.add(service.getUuid());
        }
        return uuids;
    }

    Map<UUID, Observable<byte[]>> getCharacteristicNotificationSources() {
        return characteristicNotificationSources;
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
    @SuppressWarnings("deprecation")
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

    private BleDisconnectedException handleDisconnection(int status) {
        BleDisconnectedException e = new BleDisconnectedException(deviceMock.getMacAddress(), status);
        deviceMock.disconnectWithException(e);
        return e;
    }

    @Override
    public Single<byte[]> readCharacteristic(@NonNull UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid).flatMap(new Function<BluetoothGattCharacteristic, SingleSource<? extends byte[]>>() {
            @Override
            public SingleSource<? extends byte[]> apply(final BluetoothGattCharacteristic characteristic) throws Exception {
                return readCharacteristic(characteristic);
            }
        });
    }

    @Override
    public Single<byte[]> readCharacteristic(@NonNull final BluetoothGattCharacteristic characteristic) {
        final RxBleCharacteristicReadCallback readCallback = characteristicReadCallbacks.get(characteristic.getUuid());
        if (readCallback == null) {
            return Single.just(characteristic.getValue());
        }
        return Single.just(characteristic).flatMap(new Function<BluetoothGattCharacteristic, SingleSource<? extends byte[]>>() {
            @Override
            public SingleSource<? extends byte[]> apply(final BluetoothGattCharacteristic characteristic) throws Exception {
                final SingleSubject<byte[]> subj = SingleSubject.create();
                readCallback.handle(deviceMock, characteristic, new RxBleGattReadResultMock() {
                    @Override
                    public void success(byte[] data) {
                        characteristic.setValue(data);
                        subj.onSuccess(data);
                    }

                    @Override
                    public void failure(int status) {
                        subj.onError(
                                new BleGattCharacteristicException(null,
                                        characteristic,
                                        status,
                                        BleGattOperationType.CHARACTERISTIC_READ)
                        );
                    }

                    @Override
                    public void disconnect(int status) {
                        subj.onError(handleDisconnection(status));
                    }
                });
                return subj;
            }
        }).doOnSuccess(new Consumer<byte[]>() {
            @Override
            public void accept(byte[] bytes) throws Exception {
                characteristic.setValue(bytes);
            }
        });
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
            .flatMap(new Function<BluetoothGattDescriptor, SingleSource<? extends byte[]>>() {
                @Override
                public SingleSource<? extends byte[]> apply(final BluetoothGattDescriptor descriptor) throws Exception {
                    return readDescriptor(descriptor);
                }
            });
    }

    @Override
    public Single<byte[]> readDescriptor(@NonNull final BluetoothGattDescriptor descriptor) {
        Map<UUID, RxBleDescriptorReadCallback> descriptorCallbacks = descriptorReadCallbacks
                .get(descriptor.getCharacteristic().getUuid());
        if (descriptorCallbacks != null) {
            final RxBleDescriptorReadCallback readCallback = descriptorCallbacks.get(descriptor.getUuid());
            if (readCallback != null) {
                return Single.just(descriptor).flatMap(new Function<BluetoothGattDescriptor, SingleSource<? extends byte[]>>() {
                    @Override
                    public SingleSource<? extends byte[]> apply(final BluetoothGattDescriptor descriptor) throws Exception {
                        final SingleSubject<byte[]> subj = SingleSubject.create();
                        readCallback.handle(deviceMock, descriptor, new RxBleGattReadResultMock() {
                            @Override
                            public void success(byte[] data) {
                                descriptor.setValue(data);
                                subj.onSuccess(data);
                            }

                            @Override
                            public void failure(int status) {
                                subj.onError(
                                        new BleGattDescriptorException(null,
                                                descriptor,
                                                status,
                                                BleGattOperationType.DESCRIPTOR_READ)
                                );
                            }

                            @Override
                            public void disconnect(int status) {
                                subj.onError(handleDisconnection(status));
                            }
                        });
                        return subj;
                    }
                });
            }
        }
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
    public Single<byte[]> writeCharacteristic(@NonNull BluetoothGattCharacteristic bluetoothGattCharacteristic,
                                              @NonNull final byte[] data) {
        final RxBleCharacteristicWriteCallback writeCallback = characteristicWriteCallbacks
                .get(bluetoothGattCharacteristic.getUuid());
        if (writeCallback == null) {
            bluetoothGattCharacteristic.setValue(data);
            return Single.just(data);
        }
        // wrap characteristic in single so exceptions from writeCallback are handled for us
        return Single.just(bluetoothGattCharacteristic)
                .flatMap(new Function<BluetoothGattCharacteristic, SingleSource<byte[]>>() {
                    @Override
                    public SingleSource<byte[]> apply(final BluetoothGattCharacteristic characteristic) throws Exception {
                        final SingleSubject<byte[]> subj = SingleSubject.create();
                        writeCallback.handle(deviceMock, characteristic, data, new RxBleGattWriteResultMock() {
                            @Override
                            public void success() {
                                characteristic.setValue(data);
                                subj.onSuccess(data);
                            }

                            @Override
                            public void failure(int status) {
                                subj.onError(
                                        new BleGattCharacteristicException(null,
                                                characteristic,
                                                status,
                                                BleGattOperationType.CHARACTERISTIC_WRITE)
                                );
                            }

                            @Override
                            public void disconnect(int status) {
                                subj.onError(handleDisconnection(status));
                            }
                        });
                        return subj;
                    }
                });
    }

    @Override
    public Single<byte[]> writeCharacteristic(@NonNull UUID characteristicUuid, @NonNull final byte[] data) {
        return getCharacteristic(characteristicUuid).flatMap(new Function<BluetoothGattCharacteristic, SingleSource<byte[]>>() {
            @Override
            public SingleSource<byte[]> apply(final BluetoothGattCharacteristic characteristic) throws Exception {
                return writeCharacteristic(characteristic, data);
            }
        });
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
    public Completable writeDescriptor(@NonNull final UUID serviceUuid, @NonNull final UUID characteristicUuid,
                                              @NonNull final UUID descriptorUuid, @NonNull final byte[] data) {
        return discoverServices()
            .flatMap(new Function<RxBleDeviceServices, SingleSource<BluetoothGattDescriptor>>() {
                @Override
                public SingleSource<BluetoothGattDescriptor> apply(RxBleDeviceServices rxBleDeviceServices) {
                    return rxBleDeviceServices.getDescriptor(serviceUuid, characteristicUuid, descriptorUuid);
                }
            }).flatMapCompletable(new Function<BluetoothGattDescriptor, Completable>() {
                @Override
                public Completable apply(final BluetoothGattDescriptor descriptor) throws Exception {
                    return writeDescriptor(descriptor, data);
                }
            });
    }

    @Override
    public Completable writeDescriptor(@NonNull final BluetoothGattDescriptor descriptor, @NonNull final byte[] data) {
        Map<UUID, RxBleDescriptorWriteCallback> descriptorCallbacks = descriptorWriteCallbacks
                .get(descriptor.getCharacteristic().getUuid());
        if (descriptorCallbacks != null) {
            final RxBleDescriptorWriteCallback writeCallback = descriptorCallbacks.get(descriptor.getUuid());
            if (writeCallback != null) {
                // wrap descriptor in single so exceptions from writeCallback are handled for us
                return Single.just(descriptor).flatMapCompletable(new Function<BluetoothGattDescriptor, Completable>() {
                    @Override
                    public Completable apply(final BluetoothGattDescriptor descriptor) throws Exception {
                        final CompletableSubject subj = CompletableSubject.create();
                        writeCallback.handle(deviceMock, descriptor, data, new RxBleGattWriteResultMock() {
                            @Override
                            public void success() {
                                descriptor.setValue(data);
                                subj.onComplete();
                            }

                            @Override
                            public void failure(int status) {
                                subj.onError(
                                        new BleGattDescriptorException(null, descriptor, status, BleGattOperationType.DESCRIPTOR_WRITE)
                                );
                            }

                            @Override
                            public void disconnect(int status) {
                                subj.onError(handleDisconnection(status));
                            }
                        });
                        return subj;
                    }
                });
            }
        }
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

    @Override
    public Observable<ConnectionParameters> observeConnectionParametersUpdates() {
        return Observable.never();
    }

    private void dismissCharacteristicNotification(UUID characteristicUuid, NotificationSetupMode setupMode, boolean isIndication) {
        notificationObservableMap.remove(characteristicUuid);
        final Disposable subscribe = setupCharacteristicNotification(characteristicUuid, setupMode, false, isIndication)
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

    @Override
    public <T> Observable<T> queue(@NonNull RxBleCustomOperation<T> operation, Priority priority) {
        throw new UnsupportedOperationException("Mock does not support queuing custom operation.");
    }

    public static class Builder {
        private RxBleDeviceServices rxBleDeviceServices;
        private int rssi;
        private Map<UUID, Observable<byte[]>> characteristicNotificationSources = new HashMap<>();
        private Map<UUID, RxBleCharacteristicReadCallback> characteristicReadCallbacks = new HashMap<>();
        private Map<UUID, RxBleCharacteristicWriteCallback> characteristicWriteCallbacks = new HashMap<>();
        private Map<UUID, Map<UUID, RxBleDescriptorReadCallback>> descriptorReadCallbacks = new HashMap<>();
        private Map<UUID, Map<UUID, RxBleDescriptorWriteCallback>> descriptorWriteCallbacks = new HashMap<>();

        public Builder() {

        }

        public RxBleConnectionMock build() {
            if (this.rssi == -1) throw new IllegalStateException("Rssi is required. DeviceBuilder#rssi should be called.");
            return new RxBleConnectionMock(
                    rxBleDeviceServices,
                    rssi,
                    characteristicNotificationSources,
                    characteristicReadCallbacks,
                    characteristicWriteCallbacks,
                    descriptorReadCallbacks,
                    descriptorWriteCallbacks
            );
        }

        /**
         * Set a rssi that will be reported. Calling this method is required.
         */
        public Builder rssi(int rssi) {
            this.rssi = rssi;
            this.rxBleDeviceServices = new RxBleDeviceServices(new ArrayList<BluetoothGattService>());
            return this;
        }

        /**
         * Add a {@link BluetoothGattService} to the device. Calling this method is not required.
         *
         * @param uuid            service UUID
         * @param characteristics characteristics that the service should report. Use {@link RxBleClientMock.CharacteristicsBuilder} to
         *                        create them.
         */
        public Builder addService(@NonNull UUID uuid, @NonNull List<BluetoothGattCharacteristic> characteristics) {
            BluetoothGattService bluetoothGattService = new BluetoothGattService(uuid, 0);
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                bluetoothGattService.addCharacteristic(characteristic);
            }
            rxBleDeviceServices.getBluetoothGattServices().add(bluetoothGattService);
            return this;
        }

        /**
         * Set an {@link Observable} that will be used to fire characteristic change notifications. It will be subscribed to after
         * a call to {@link com.polidea.rxandroidble2.RxBleConnection#setupNotification(UUID)}. Calling this method is not required.
         *
         * @param characteristicUUID UUID of the characteristic that will be observed for notifications
         * @param sourceObservable   Observable that will be subscribed to in order to receive characteristic change notifications
         */
        public Builder notificationSource(@NonNull UUID characteristicUUID, @NonNull Observable<byte[]> sourceObservable) {
            characteristicNotificationSources.put(characteristicUUID, sourceObservable);
            return this;
        }

        /**
         * Set a {@link Function} that will be used to handle characteristic reads for characteristics with a given UUID. The
         * function should return a Single which will emit the read data when complete. Calling this method is not required.
         * @param characteristicUUID UUID of the characteristic that the callback will handle reads for
         * @param readCallback The callback
         */
        public Builder characteristicReadCallback(@NonNull UUID characteristicUUID,
                                                  @NonNull RxBleCharacteristicReadCallback readCallback) {
            characteristicReadCallbacks.put(characteristicUUID, readCallback);
            return this;
        }

        /**
         * Set a {@link Function} that will be used to handle characteristic writes for characteristics with a given UUID. The
         * function should return a Completable that completes when the write completes. Calling this method is not required.
         * @param characteristicUUID UUID of the characteristic that the callback will handle reads for
         * @param writeCallback The callback
         */
        public Builder characteristicWriteCallback(@NonNull UUID characteristicUUID,
                                                   @NonNull RxBleCharacteristicWriteCallback writeCallback) {
            characteristicWriteCallbacks.put(characteristicUUID, writeCallback);
            return this;
        }

        /**
         * Set a {@link Function} that will be used to handle descriptor reads for descriptors with a given UUID. The
         * function should return a Single which will emit the read data when complete. Calling this method is not required.
         * @param characteristicUUID UUID of the characteristic that the descriptor is used in
         * @param descriptorUUID UUID of the descriptor that the callback will handle reads for
         * @param readCallback The callback
         */
        public Builder descriptorReadCallback(@NonNull UUID characteristicUUID,
                                              @NonNull UUID descriptorUUID,
                                              @NonNull RxBleDescriptorReadCallback readCallback) {
            Map<UUID, RxBleDescriptorReadCallback> descriptorCallbacks = descriptorReadCallbacks
                    .get(characteristicUUID);
            if (descriptorCallbacks == null) {
                descriptorCallbacks = new HashMap<>();
                descriptorReadCallbacks.put(characteristicUUID, descriptorCallbacks);
            }
            descriptorCallbacks.put(descriptorUUID, readCallback);
            return this;
        }

        /**
         * Set a {@link Function} that will be used to handle descriptor writes for descriptors with a given UUID. The
         * function should return a Completable that completes when the write completes. Calling this method is not required.
         * @param characteristicUUID UUID of the characteristic that the descriptor is used in
         * @param descriptorUUID UUID of the descriptor that the callback will handle reads for
         * @param writeCallback The callback
         */
        public Builder descriptorWriteCallback(@NonNull UUID characteristicUUID,
                                               @NonNull UUID descriptorUUID,
                                               @NonNull RxBleDescriptorWriteCallback writeCallback) {
            Map<UUID, RxBleDescriptorWriteCallback> descriptorCallbacks = descriptorWriteCallbacks
                    .get(characteristicUUID);
            if (descriptorCallbacks == null) {
                descriptorCallbacks = new HashMap<>();
                descriptorWriteCallbacks.put(characteristicUUID, descriptorCallbacks);
            }
            descriptorCallbacks.put(descriptorUUID, writeCallback);
            return this;
        }
    }
}
