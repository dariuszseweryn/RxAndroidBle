package com.polidea.rxandroidble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.exceptions.BleCharacteristicNotFoundException;
import com.polidea.rxandroidble.exceptions.BleServiceNotFoundException;

import java.util.List;
import java.util.UUID;

import java.util.concurrent.Callable;
import rx.Observable;
import rx.functions.Func1;

/**
 * Service discovery result containing list of services and characteristics within the services.
 */
public class RxBleDeviceServices {

    private final List<BluetoothGattService> bluetoothGattServices;

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
    public Observable<BluetoothGattService> getService(@NonNull final UUID serviceUuid) {
        return Observable.from(bluetoothGattServices)
                .takeFirst(new Func1<BluetoothGattService, Boolean>() {
                    @Override
                    public Boolean call(BluetoothGattService bluetoothGattService) {
                        return bluetoothGattService.getUuid().equals(serviceUuid);
                    }
                })
                .switchIfEmpty(Observable.<BluetoothGattService>error(new BleServiceNotFoundException(serviceUuid)));
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
    public Observable<BluetoothGattCharacteristic> getCharacteristic(@NonNull final UUID characteristicUuid) {
        return Observable.fromCallable(new Callable<BluetoothGattCharacteristic>() {
            @Override
            public BluetoothGattCharacteristic call() throws Exception {
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
    public Observable<BluetoothGattCharacteristic> getCharacteristic(@NonNull UUID serviceUuid, @NonNull final UUID characteristicUuid) {
        return getService(serviceUuid)
                .map(new Func1<BluetoothGattService, BluetoothGattCharacteristic>() {
                    @Override
                    public BluetoothGattCharacteristic call(BluetoothGattService bluetoothGattService) {
                        return bluetoothGattService.getCharacteristic(characteristicUuid);
                    }
                })
                .takeFirst(new Func1<BluetoothGattCharacteristic, Boolean>() {
                    @Override
                    public Boolean call(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
                        return bluetoothGattCharacteristic != null;
                    }
                })
                .switchIfEmpty(Observable.<BluetoothGattCharacteristic>error(new BleCharacteristicNotFoundException(characteristicUuid)));
    }

    // TODO: [PU] 15.03.2016 Consider moving getDescriptor to the characteristic
    public Observable<BluetoothGattDescriptor> getDescriptor(final UUID characteristicUuid, final UUID descriptorUuid) {
        return getCharacteristic(characteristicUuid)
                .map(new Func1<BluetoothGattCharacteristic, BluetoothGattDescriptor>() {
                    @Override
                    public BluetoothGattDescriptor call(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
                        return bluetoothGattCharacteristic.getDescriptor(descriptorUuid);
                    }
                })
                .filter(new Func1<Object, Boolean>() {
                    @Override
                    public Boolean call(Object bluetoothGattDescriptor) {
                        return bluetoothGattDescriptor != null;
                    }
                });
    }

    public Observable<BluetoothGattDescriptor> getDescriptor(
            final UUID serviceUuid, final UUID characteristicUuid, final UUID descriptorUuid
    ) {
        return getService(serviceUuid)
                .map(new Func1<BluetoothGattService, BluetoothGattCharacteristic>() {
                    @Override
                    public BluetoothGattCharacteristic call(BluetoothGattService bluetoothGattService) {
                        return bluetoothGattService.getCharacteristic(characteristicUuid);
                    }
                })
                .map(new Func1<BluetoothGattCharacteristic, BluetoothGattDescriptor>() {
                    @Override
                    public BluetoothGattDescriptor call(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
                        return bluetoothGattCharacteristic.getDescriptor(descriptorUuid);
                    }
                });
    }
}
