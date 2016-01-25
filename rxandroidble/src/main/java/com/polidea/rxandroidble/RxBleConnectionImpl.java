package com.polidea.rxandroidble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import rx.Observable;

public class RxBleConnectionImpl implements RxBleConnection {

    private final BluetoothDevice bluetoothDevice;

    private RxBleGattCallback gattCallback = new RxBleGattCallback();

    private BluetoothGatt bluetoothGatt;

    public RxBleConnectionImpl(BluetoothDevice bluetoothDevice) {

        this.bluetoothDevice = bluetoothDevice;
    }

    public Observable<RxBleConnection> connect(Context context) {
        return Observable.create(subscriber -> {
            bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback.getBluetoothGattCallback()); // TODO: share connection?
            gattCallback
                    .getOnConnectionStateChange()
                    .filter(rxBleConnectionState -> rxBleConnectionState == RxBleConnectionState.CONNECTED)
                    .subscribe(rxBleConnectionState1 -> subscriber.onNext(this));
        });
    }

    public Observable<Map<UUID, Set<UUID>>> discoverServices() {
        return Observable.create(subscriber -> {
            gattCallback.getOnServicesDiscovered().subscribe(subscriber);
            bluetoothGatt.discoverServices();
        });
    }

    private Observable<BluetoothGattCharacteristic> getCharacteristic(UUID characteristicUuid) {
        return Observable.create(subscriber -> {
            discoverServices()
                    .flatMap(uuidSetMap -> Observable.from(uuidSetMap.entrySet()))
                    .filter(uuidSetEntry -> uuidSetEntry.getValue().contains(characteristicUuid))
                    .map(uuidSetEntry -> bluetoothGatt.getService(uuidSetEntry.getKey()).getCharacteristic(characteristicUuid))
                    .subscribe(subscriber);
        });
    }

    public Observable<Observable<byte[]>> getNotification(UUID characteristicUuid) {
        return null;
    }

    public Observable<byte[]> readCharacteristic(UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid)
                .flatMap(bluetoothGattCharacteristic -> {
                    bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
                    return gattCallback
                            .getOnCharacteristicRead()
                            .filter(uuidPair -> uuidPair.first.equals(characteristicUuid))
                            .map(uuidPair1 -> uuidPair1.second);
                });
    }

    public Observable<byte[]> writeCharacteristic(UUID characteristicUuid, byte[] data) {
        return getCharacteristic(characteristicUuid)
                .flatMap(bluetoothGattCharacteristic -> {
                    bluetoothGattCharacteristic.setValue(data);
                    bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
                    return gattCallback
                            .getOnCharacteristicWrite()
                            .filter(uuidPair -> uuidPair.first.equals(characteristicUuid))
                            .map(uuidPair1 -> uuidPair1.second);
                });
    }

    public Observable<byte[]> readDescriptor(UUID descriptorUuid) {
        return null;
    }

    public Observable<byte[]> writeDescriptor(UUID descriptorUuid, byte[] data) {
        return null;
    }

    public Observable<Integer> readRssi() {
        return null;
    }
}
