package com.polidea.rxandroidble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.List;
import java.util.UUID;

import rx.Observable;

public class RxBleDeviceServices {

    private final List<BluetoothGattService> bluetoothGattServices;

    public RxBleDeviceServices(List<BluetoothGattService> bluetoothGattServices) {
        this.bluetoothGattServices = bluetoothGattServices;
    }

    public List<BluetoothGattService> getBluetoothGattServices() {
        return bluetoothGattServices;
    }

    public Observable<BluetoothGattService> getService(UUID serviceUuid) {
        // TODO: [PU] 29.01.2016 Will raise NoSuchElementException if services are empty. It should be mapped to error if not found
        return Observable.from(bluetoothGattServices)
                .filter(bluetoothGattService -> bluetoothGattService.getUuid().equals(serviceUuid))
                .first();
    }

    public Observable<BluetoothGattCharacteristic> getCharacteristic(UUID characteristicUuid) {
        // TODO: [PU] 29.01.2016 Theoretically it may happen that characteristic UUID in duplicated in another service.
        return Observable.from(bluetoothGattServices)
                .map(bluetoothGattService -> bluetoothGattService.getCharacteristic(characteristicUuid))
                .filter(bluetoothGattCharacteristic -> bluetoothGattCharacteristic != null)
                .take(1); // Services may be empty
    }

    public Observable<BluetoothGattCharacteristic> getCharacteristic(UUID serviceUuid, UUID characteristicUuid) {
        return getService(serviceUuid).map(bluetoothGattService -> bluetoothGattService.getCharacteristic(characteristicUuid));
    }

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
