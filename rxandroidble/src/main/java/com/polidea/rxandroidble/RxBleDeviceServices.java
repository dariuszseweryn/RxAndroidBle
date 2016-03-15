package com.polidea.rxandroidble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.List;
import java.util.UUID;

import rx.Observable;

// TODO: [PU] 15.03.2016 Documentation
public class RxBleDeviceServices {

    private final List<BluetoothGattService> bluetoothGattServices;

    public RxBleDeviceServices(List<BluetoothGattService> bluetoothGattServices) {
        this.bluetoothGattServices = bluetoothGattServices;
    }

    public List<BluetoothGattService> getBluetoothGattServices() {
        return bluetoothGattServices;
    }

    public Observable<BluetoothGattService> getService(UUID serviceUuid) {
        return Observable.from(bluetoothGattServices)
                .filter(bluetoothGattService -> bluetoothGattService.getUuid().equals(serviceUuid))
                .take(1);
    }

    public Observable<BluetoothGattCharacteristic> getCharacteristic(UUID characteristicUuid) {
        return Observable.from(bluetoothGattServices).compose(extractCharacteristic(characteristicUuid));
    }

    public Observable<BluetoothGattCharacteristic> getCharacteristic(UUID serviceUuid, UUID characteristicUuid) {
        return getService(serviceUuid).compose(extractCharacteristic(characteristicUuid));
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
