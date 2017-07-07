package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;
import android.os.DeadObjectException;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.NotificationSetupMode;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.RxBleRadioOperationCustom;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.internal.RadioReleaseInterface;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.RxBleRadioOperation;
import com.polidea.rxandroidble.internal.operations.OperationsProvider;
import com.polidea.rxandroidble.internal.util.ByteAssociation;

import com.polidea.rxandroidble.internal.util.RadioReleasingEmitterWrapper;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import rx.Completable;
import rx.Emitter;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

@ConnectionScope
public class RxBleConnectionImpl implements RxBleConnection {

    private final RxBleRadio rxBleRadio;
    private final RxBleGattCallback gattCallback;
    private final BluetoothGatt bluetoothGatt;
    private final OperationsProvider operationsProvider;
    private final Provider<LongWriteOperationBuilder> longWriteOperationBuilderProvider;
    private final Scheduler callbackScheduler;
    private final ServiceDiscoveryManager serviceDiscoveryManager;
    private final NotificationAndIndicationManager notificationIndicationManager;
    private final DescriptorWriter descriptorWriter;

    private int currentMtu = GATT_MTU_MINIMUM; // Default value at the beginning

    @Inject
    public RxBleConnectionImpl(
            RxBleRadio rxBleRadio,
            RxBleGattCallback gattCallback,
            BluetoothGatt bluetoothGatt,
            ServiceDiscoveryManager serviceDiscoveryManager,
            NotificationAndIndicationManager notificationIndicationManager,
            DescriptorWriter descriptorWriter,
            OperationsProvider operationProvider,
            Provider<LongWriteOperationBuilder> longWriteOperationBuilderProvider,
            @Named(ClientComponent.NamedSchedulers.RADIO_OPERATIONS) Scheduler callbackScheduler
    ) {
        this.rxBleRadio = rxBleRadio;
        this.gattCallback = gattCallback;
        this.bluetoothGatt = bluetoothGatt;
        this.serviceDiscoveryManager = serviceDiscoveryManager;
        this.notificationIndicationManager = notificationIndicationManager;
        this.descriptorWriter = descriptorWriter;
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
    public Completable requestConnectionPriority(int connectionPriority, long delay, @NonNull TimeUnit timeUnit) {
        if (connectionPriority != BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER
                && connectionPriority != BluetoothGatt.CONNECTION_PRIORITY_BALANCED
                && connectionPriority != BluetoothGatt.CONNECTION_PRIORITY_HIGH) {
            return Completable.error(
                    new IllegalArgumentException(
                            "Connection priority must have valid value from BluetoothGatt (received "
                                    + connectionPriority + ")"
                    )
            );
        }

        if (delay <= 0) {
            return Completable.error(new IllegalArgumentException("Delay must be bigger than 0"));
        }

        return rxBleRadio
                .queue(operationsProvider.provideConnectionPriorityChangeOperation(connectionPriority, delay, timeUnit))
                .toCompletable();
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
    public int getMtu() {
        return currentMtu;
    }

    @Override
    public Observable<RxBleDeviceServices> discoverServices() {
        return serviceDiscoveryManager.getDiscoverServicesObservable(20L, TimeUnit.SECONDS);
    }

    @Override
    public Observable<RxBleDeviceServices> discoverServices(long timeout, @NonNull TimeUnit timeUnit) {
        return serviceDiscoveryManager.getDiscoverServicesObservable(timeout, timeUnit);
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
    public Observable<Observable<byte[]>> setupNotification(@NonNull UUID characteristicUuid,
                                                            @NonNull final NotificationSetupMode setupMode) {
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
                                                            @NonNull NotificationSetupMode setupMode) {
        return notificationIndicationManager.setupServerInitiatedCharacteristicRead(characteristic, setupMode, false);
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
        return notificationIndicationManager.setupServerInitiatedCharacteristicRead(characteristic, setupMode, true);
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
    public Observable<byte[]> readDescriptor(@NonNull final UUID serviceUuid, @NonNull final UUID characteristicUuid,
                                             @NonNull final UUID descriptorUuid) {
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
    public Observable<byte[]> readDescriptor(@NonNull BluetoothGattDescriptor descriptor) {
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
            @NonNull final UUID serviceUuid, @NonNull final UUID characteristicUuid, @NonNull final UUID descriptorUuid,
            @NonNull final byte[] data
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
    public Observable<byte[]> writeDescriptor(@NonNull BluetoothGattDescriptor bluetoothGattDescriptor, @NonNull byte[] data) {
        return descriptorWriter.writeDescriptor(bluetoothGattDescriptor, data);
    }

    @Override
    public Observable<Integer> readRssi() {
        return rxBleRadio.queue(operationsProvider.provideRssiReadOperation());
    }

    @Override
    public <T> Observable<T> queue(@NonNull final RxBleRadioOperationCustom<T> operation) {
        return rxBleRadio.queue(new RxBleRadioOperation<T>() {
            @Override
            @SuppressWarnings("ConstantConditions")
            protected void protectedRun(final Emitter<T> emitter, final RadioReleaseInterface radioReleaseInterface) throws Throwable {
                final Observable<T> operationObservable;

                try {
                    operationObservable = operation.asObservable(bluetoothGatt, gattCallback, callbackScheduler);
                } catch (Throwable throwable) {
                    radioReleaseInterface.release();
                    throw throwable;
                }

                if (operationObservable == null) {
                    radioReleaseInterface.release();
                    throw new IllegalArgumentException("The custom operation asObservable method must return a non-null observable");
                }

                final RadioReleasingEmitterWrapper<T> emitterWrapper = new RadioReleasingEmitterWrapper<>(emitter, radioReleaseInterface);
                operationObservable.subscribe(emitterWrapper);
            }

            @Override
            protected BleException provideException(DeadObjectException deadObjectException) {
                return new BleDisconnectedException(deadObjectException, bluetoothGatt.getDevice().getAddress());
            }
        });
    }
}
