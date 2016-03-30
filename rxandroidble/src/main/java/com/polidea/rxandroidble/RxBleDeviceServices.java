package com.polidea.rxandroidble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.exceptions.BleCharacteristicNotFoundException;
import com.polidea.rxandroidble.exceptions.BleServiceNotFoundException;

import java.util.List;
import java.util.UUID;

import rx.Observable;

/**
 * Service discovery result containing list of services and characteristics withing the services.
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
    public Observable<BluetoothGattService> getService(@NonNull UUID serviceUuid) {
        return Observable.from(bluetoothGattServices)
                .filter(bluetoothGattService -> bluetoothGattService.getUuid().equals(serviceUuid))
                .take(1)
                .switchIfEmpty(Observable.error(new BleServiceNotFoundException(serviceUuid)));
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
    public Observable<BluetoothGattCharacteristic> getCharacteristic(@NonNull UUID characteristicUuid) {
        return Observable.from(bluetoothGattServices)
                .compose(extractCharacteristic(characteristicUuid))
                .take(1)
                .switchIfEmpty(Observable.error(new BleCharacteristicNotFoundException(characteristicUuid)));
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
    public Observable<BluetoothGattCharacteristic> getCharacteristic(@NonNull UUID serviceUuid, @NonNull UUID characteristicUuid) {
        return getService(serviceUuid)
                .compose(extractCharacteristic(characteristicUuid))
                .take(1)
                .switchIfEmpty(Observable.error(new BleCharacteristicNotFoundException(characteristicUuid)));
    }

    private Observable.Transformer<BluetoothGattService, BluetoothGattCharacteristic> extractCharacteristic(UUID characteristicUuid) {
        return source -> {
            return source.map(bluetoothGattService -> bluetoothGattService.getCharacteristic(characteristicUuid))
                    .filter(bluetoothGattCharacteristic -> bluetoothGattCharacteristic != null)
                    .take(1); // Services may be empty;
        };
    }

    // TODO: [PU] 15.03.2016 Consider moving getDescriptor to the characteristic
    public Observable<BluetoothGattDescriptor> getDescriptor(UUID characteristicUuid, UUID descriptorUuid) {
        return getCharacteristic(characteristicUuid)
                .map(bluetoothGattCharacteristic -> bluetoothGattCharacteristic.getDescriptor(descriptorUuid))
                .filter(bluetoothGattDescriptor -> bluetoothGattDescriptor != null);
    }

    public Observable<BluetoothGattDescriptor> getDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid) {
        return getService(serviceUuid)
                .map(bluetoothGattService -> bluetoothGattService.getCharacteristic(characteristicUuid))
                .map(bluetoothGattCharacteristic -> bluetoothGattCharacteristic.getDescriptor(descriptorUuid));
    }
}
