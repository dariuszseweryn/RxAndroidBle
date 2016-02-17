package com.polidea.rxandroidble.mockrxandroidble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.internal.RxBleConnectibleConnection;

import java.util.UUID;

import rx.Observable;

class RxBleConnectionMock implements RxBleConnectibleConnection {

    private RxBleDeviceServices rxBleDeviceServices;
    private Integer rssid;

    public RxBleConnectionMock(RxBleDeviceServices rxBleDeviceServices, Integer rssid) {
        this.rxBleDeviceServices = rxBleDeviceServices;
        this.rssid = rssid;
    }

    private Observable<BluetoothGattCharacteristic> getCharacteristic(UUID characteristicUuid) {
        return discoverServices()
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic(characteristicUuid));
    }
    @Override
    public Observable<RxBleConnection> connect(Context context) {
        return Observable.just((RxBleConnection) this);
    }

    @Override
    public Observable<RxBleDeviceServices> discoverServices() {
        return Observable.just(rxBleDeviceServices);
    }

    @Override
    public Observable<Observable<byte[]>> getNotification(UUID characteristicUuid) {
        throw new UnsupportedOperationException("Not supported yet!");
    }

    @Override
    public Observable<byte[]> readCharacteristic(UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid).map(BluetoothGattCharacteristic::getValue);
    }

    @Override
    public Observable<byte[]> writeCharacteristic(UUID characteristicUuid, byte[] data) {
        getCharacteristic(characteristicUuid).map(characteristic -> characteristic.setValue(data)).subscribe();
        return Observable.just(data);
    }

    @Override
    public Observable<byte[]> readDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid) {
        return discoverServices()
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getDescriptor(serviceUuid, characteristicUuid, descriptorUuid))
                .map(BluetoothGattDescriptor::getValue);
    }

    @Override
    public Observable<byte[]> writeDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, byte[] data) {
        discoverServices()
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getDescriptor(serviceUuid, characteristicUuid, descriptorUuid))
                .map(bluetoothGattDescriptor -> bluetoothGattDescriptor.setValue(data)).subscribe();
        return Observable.just(data);
    }

    @Override
    public Observable<Integer> readRssi() {
        return Observable.just(rssid);
    }
}
