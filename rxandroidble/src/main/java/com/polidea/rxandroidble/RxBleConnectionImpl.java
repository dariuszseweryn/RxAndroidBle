package com.polidea.rxandroidble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationReadRssi;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationServicesDiscover;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import rx.Observable;

public class RxBleConnectionImpl implements RxBleConnection {

    private final BluetoothDevice bluetoothDevice;

    private final RxBleRadio rxBleRadio;

    private RxBleGattCallback gattCallback = new RxBleGattCallback();

    private final AtomicReference<BluetoothGatt> bluetoothGattAtomicReference = new AtomicReference<>();

    public RxBleConnectionImpl(BluetoothDevice bluetoothDevice, RxBleRadio rxBleRadio) {

        this.bluetoothDevice = bluetoothDevice;
        this.rxBleRadio = rxBleRadio;
    }

    public Observable<RxBleConnection> connect(Context context) {
        final RxBleRadioOperationConnect operationConnect = new RxBleRadioOperationConnect(context, bluetoothDevice, gattCallback, this);
        final Observable<RxBleConnection> observable = operationConnect.asObservable();
        final AtomicReference<RxBleRadioOperationDisconnect> disconnectAtomicReference = new AtomicReference<>();
        return observable
                .doOnSubscribe(() -> rxBleRadio.queue(operationConnect))
                .doOnNext(rxBleConnection -> {
                    bluetoothGattAtomicReference.set(operationConnect.getBluetoothGatt());
                    disconnectAtomicReference.set(
                            new RxBleRadioOperationDisconnect(
                                    gattCallback,
                                    bluetoothGattAtomicReference.get(),
                                    (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)
                            )
                    );
                })
                .doOnError(throwable -> rxBleRadio.queue(disconnectAtomicReference.get()))
                .doOnUnsubscribe(() -> rxBleRadio.queue(disconnectAtomicReference.get()));
    }

    public Observable<Map<UUID, Set<UUID>>> discoverServices() {
        final BluetoothGatt bluetoothGatt = this.bluetoothGattAtomicReference.get();
        final RxBleRadioOperationServicesDiscover operationServicesDiscover = new RxBleRadioOperationServicesDiscover(gattCallback, bluetoothGatt);
        final Observable<Map<UUID, Set<UUID>>> observable = operationServicesDiscover.asObservable();
        return observable.doOnSubscribe(() -> rxBleRadio.queue(operationServicesDiscover));
    }

    private Observable<BluetoothGattCharacteristic> getCharacteristic(UUID characteristicUuid) {
        return Observable.create(subscriber ->
                        discoverServices()
                                .flatMap(uuidSetMap -> Observable.from(uuidSetMap.entrySet()))
                                .filter(uuidSetEntry -> uuidSetEntry.getValue().contains(characteristicUuid))
                                .map(uuidSetEntry -> bluetoothGattAtomicReference.get().getService(uuidSetEntry.getKey()).getCharacteristic(characteristicUuid))
                                .subscribe(subscriber)
        );
    }

    public Observable<Observable<byte[]>> getNotification(UUID characteristicUuid) {
        return null; // TODO
    }

    public Observable<byte[]> readCharacteristic(UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid)
                .flatMap(bluetoothGattCharacteristic -> {
                    bluetoothGattAtomicReference.get().readCharacteristic(bluetoothGattCharacteristic);
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
                    bluetoothGattAtomicReference.get().writeCharacteristic(bluetoothGattCharacteristic);
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
        final RxBleRadioOperationReadRssi operationReadRssi = new RxBleRadioOperationReadRssi(gattCallback, bluetoothGattAtomicReference.get());
        final Observable<Integer> observable = operationReadRssi.asObservable();
        return observable.doOnSubscribe(() -> rxBleRadio.queue(operationReadRssi));
    }
}
