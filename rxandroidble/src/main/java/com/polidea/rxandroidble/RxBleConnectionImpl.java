package com.polidea.rxandroidble;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import rx.Observable;
import rx.subjects.PublishSubject;

public class RxBleConnectionImpl implements RxBleConnection {

    private final BluetoothAdapter bluetoothAdapter;

    public RxBleConnectionImpl(BluetoothAdapter bluetoothAdapter) {

        this.bluetoothAdapter = bluetoothAdapter;
    }

    public Observable<RxBleConnection> connect(Context context) {
        return Observable.create(subscriber -> {});
    }

    public Observable<Map<UUID, Set<UUID>>> discoverServices() {
        return null;
    }

    public Observable<Observable<byte[]>> getNotification(UUID characteristicUuid) {
        return null;
    }

    public Observable<byte[]> readCharacteristic(UUID characteristicUuid) {
        return null;
    }

    public Observable<byte[]> writeCharacteristic(UUID characteristicUuid, byte[] data) {
        return null;
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
