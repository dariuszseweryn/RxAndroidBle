package com.polidea.rxandroidble2.internal.operations;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import androidx.annotation.NonNull;

import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble2.exceptions.BleGattOperationType;
import com.polidea.rxandroidble2.internal.SingleResponseOperation;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble2.internal.logger.LoggerUtilBluetoothServices;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleSource;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Supplier;

public class ServiceDiscoveryOperation extends SingleResponseOperation<RxBleDeviceServices> {

    final BluetoothGatt bluetoothGatt;
    final LoggerUtilBluetoothServices bleServicesLogger;

    ServiceDiscoveryOperation(
            RxBleGattCallback rxBleGattCallback,
            BluetoothGatt bluetoothGatt,
            LoggerUtilBluetoothServices bleServicesLogger,
            TimeoutConfiguration timeoutConfiguration) {
        super(bluetoothGatt, rxBleGattCallback, BleGattOperationType.SERVICE_DISCOVERY, timeoutConfiguration);
        this.bluetoothGatt = bluetoothGatt;
        this.bleServicesLogger = bleServicesLogger;
    }

    @Override
    protected Single<RxBleDeviceServices> getCallback(RxBleGattCallback rxBleGattCallback) {
        return rxBleGattCallback.getOnServicesDiscovered().firstOrError()
                .doOnSuccess(new Consumer<RxBleDeviceServices>() {
                    @Override
                    public void accept(RxBleDeviceServices rxBleDeviceServices) {
                        bleServicesLogger.log(rxBleDeviceServices, bluetoothGatt.getDevice());
                    }
                });
    }

    @Override
    protected boolean startOperation(BluetoothGatt bluetoothGatt) {
        return bluetoothGatt.discoverServices();
    }

    /**
     * Sometimes it happens that the {@link BluetoothGatt} will receive all {@link BluetoothGattService}'s,
     * {@link android.bluetooth.BluetoothGattCharacteristic}'s and {@link android.bluetooth.BluetoothGattDescriptor}
     * but it won't receive the final callback that the service discovery was completed. This is a potential workaround.
     * <p>
     * There is a change in Android 7.0.0_r1 where all data is received at once - in this situation returned services size will be always 0
     * https://android.googlesource.com/platform/frameworks/base/+/android-7.0.0_r1/core/java/android/bluetooth/BluetoothGatt.java#206
     * https://android.googlesource.com/platform/frameworks/base/+/android-6.0.1_r72/core/java/android/bluetooth/BluetoothGatt.java#205
     *
     * @param bluetoothGatt     the BluetoothGatt to use
     * @param rxBleGattCallback the RxBleGattCallback to use
     * @param timeoutScheduler  the Scheduler for timeout to use
     * @return Observable that may emit {@link RxBleDeviceServices} or {@link TimeoutException}
     */
    @NonNull
    @Override
    protected Single<RxBleDeviceServices> timeoutFallbackProcedure(
            final BluetoothGatt bluetoothGatt,
            final RxBleGattCallback rxBleGattCallback,
            final Scheduler timeoutScheduler
    ) {
        return Single.defer(new Supplier<SingleSource<? extends RxBleDeviceServices>>() {
            @Override
            public SingleSource<? extends RxBleDeviceServices> get() {
                final List<BluetoothGattService> services = bluetoothGatt.getServices();
                if (services.size() == 0) {
                    // if after the timeout services are empty we have no other option to declare a failed discovery
                    return Single.error(new BleGattCallbackTimeoutException(bluetoothGatt, BleGattOperationType.SERVICE_DISCOVERY));
                } else {
                /*
                it is observed that usually the Android OS is returning services, characteristics and descriptors in a short period of time
                if there are some services available we will wait for 5 more seconds just to be sure that
                the timeout was not triggered right in the moment of filling the services and then emit a value.
                 */
                    return Single
                            .timer(5, TimeUnit.SECONDS, timeoutScheduler)
                            .flatMap(new Function<Long, Single<RxBleDeviceServices>>() {
                                @Override
                                public Single<RxBleDeviceServices> apply(Long delayedSeconds) {
                                    return Single.fromCallable(new Callable<RxBleDeviceServices>() {
                                        @Override
                                        public RxBleDeviceServices call() {
                                            return new RxBleDeviceServices(bluetoothGatt.getServices());
                                        }
                                    });
                                }
                            });
                }
            }
        });
    }

    @Override
    @NonNull
    public String toString() {
        return "ServiceDiscoveryOperation{" + super.toString() + '}';
    }
}
