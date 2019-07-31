package com.polidea.rxandroidble2;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import androidx.annotation.NonNull;

import com.polidea.rxandroidble2.exceptions.BleCharacteristicNotFoundException;
import com.polidea.rxandroidble2.exceptions.BleDescriptorNotFoundException;
import com.polidea.rxandroidble2.exceptions.BleServiceNotFoundException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Predicate;

/**
 * Service discovery result containing list of services and characteristics within the services.
 */
public class RxBleDeviceServices {

    final List<BluetoothGattService> bluetoothGattServices;

    public RxBleDeviceServices(List<BluetoothGattService> bluetoothGattServices) {
        this.bluetoothGattServices = bluetoothGattServices;
    }

    /**
     * List of all GATT services supported by the device
     */
    public List<BluetoothGattService> getBluetoothGattServices() {
        return bluetoothGattServices;
    }

    /**
     * Creates an observable emitting {@link BluetoothGattService} with matching service UUID.
     * The observable completes after first emission.
     *
     * @param serviceUuid Service UUID to be found
     * @return Observable emitting matching services or error if hasn't been found.
     * @throws BleServiceNotFoundException if service with given UUID hasn't been found.
     */
    public Single<BluetoothGattService> getService(@NonNull final UUID serviceUuid) {
        return Observable.fromIterable(bluetoothGattServices)
                .filter(new Predicate<BluetoothGattService>() {

                    @Override
                    public boolean test(BluetoothGattService bluetoothGattService) {
                        return bluetoothGattService.getUuid().equals(serviceUuid);
                    }
                })
                .firstElement()
                .switchIfEmpty(Single.<BluetoothGattService>error(new BleServiceNotFoundException(serviceUuid)));
    }

    /**
     * Creates an observable emitting {@link BluetoothGattCharacteristic} with matching characteristic UUID.
     * The observable completes after first emission.
     * <p>
     * The main assumption is that characteristics have unique UUID across all services as there is a traversal done
     * across all of them. For an alternative see RxBleDeviceServices#getCharacteristic(UUID)
     *
     * @param characteristicUuid Characteristic UUID to be found
     * @return Observable emitting matching characteristic or error if hasn't been found.
     * @throws BleCharacteristicNotFoundException if characteristic with given UUID hasn't been found.
     */
    public Single<BluetoothGattCharacteristic> getCharacteristic(@NonNull final UUID characteristicUuid) {
        return Single.fromCallable(new Callable<BluetoothGattCharacteristic>() {
            @Override
            public BluetoothGattCharacteristic call() {
                for (BluetoothGattService service : bluetoothGattServices) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
                    if (characteristic != null) {
                        return characteristic;
                    }
                }
                throw new BleCharacteristicNotFoundException(characteristicUuid);
            }
        });
    }

    /**
     * Creates an observable emitting {@link BluetoothGattCharacteristic}s with matching service UUID and characteristic UUID.
     * The observable completes after first emission.
     *
     * @param characteristicUuid Characteristic UUID to be found
     * @param serviceUuid        Service UUID to search in
     * @return Observable emitting matching characteristic or error if hasn't been found.
     * @throws BleCharacteristicNotFoundException if characteristic with given UUID hasn't been found.
     * @see RxBleDeviceServices#getCharacteristic(UUID)
     */
    public Single<BluetoothGattCharacteristic> getCharacteristic(@NonNull UUID serviceUuid, @NonNull final UUID characteristicUuid) {
        return getService(serviceUuid)
                .map(new Function<BluetoothGattService, BluetoothGattCharacteristic>() {
                    @Override
                    public BluetoothGattCharacteristic apply(BluetoothGattService bluetoothGattService) {
                        final BluetoothGattCharacteristic characteristic = bluetoothGattService.getCharacteristic(characteristicUuid);

                        if (characteristic == null) {
                            throw new BleCharacteristicNotFoundException(characteristicUuid);
                        }
                        return characteristic;
                    }
                });
    }

    public Single<BluetoothGattDescriptor> getDescriptor(final UUID characteristicUuid, final UUID descriptorUuid) {
        return getCharacteristic(characteristicUuid)
                .map(new Function<BluetoothGattCharacteristic, BluetoothGattDescriptor>() {
                    @Override
                    public BluetoothGattDescriptor apply(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
                        final BluetoothGattDescriptor descriptor = bluetoothGattCharacteristic.getDescriptor(descriptorUuid);

                        if (descriptor == null) {
                            throw new BleDescriptorNotFoundException(descriptorUuid);
                        }

                        return descriptor;
                    }
                });
    }

    public Single<BluetoothGattDescriptor> getDescriptor(
            final UUID serviceUuid, final UUID characteristicUuid, final UUID descriptorUuid
    ) {
        return getService(serviceUuid)
                .map(new Function<BluetoothGattService, BluetoothGattCharacteristic>() {
                    @Override
                    public BluetoothGattCharacteristic apply(BluetoothGattService bluetoothGattService) {
                        return bluetoothGattService.getCharacteristic(characteristicUuid);
                    }
                })
                .map(new Function<BluetoothGattCharacteristic, BluetoothGattDescriptor>() {
                    @Override
                    public BluetoothGattDescriptor apply(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
                        final BluetoothGattDescriptor descriptor = bluetoothGattCharacteristic.getDescriptor(descriptorUuid);

                        if (descriptor == null) {
                            throw new BleDescriptorNotFoundException(descriptorUuid);
                        }

                        return descriptor;
                    }
                });
    }
}
