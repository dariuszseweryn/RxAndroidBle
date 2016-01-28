package com.polidea.rxandroidble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationCharacteristicRead;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationServicesDiscoverGetCached;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationReadRssi;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationServicesDiscover;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import rx.Observable;

public class RxBleConnectionImpl implements RxBleConnection {

    private final BluetoothDevice bluetoothDevice;

    private final RxBleRadio rxBleRadio;

    private RxBleGattCallback gattCallback = new RxBleGattCallback();

    private final AtomicReference<BluetoothGatt> bluetoothGattAtomicReference = new AtomicReference<>();

    private AtomicReference<RxBleDeviceServices> discoveredServicesAtomicReference = new AtomicReference<>();

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

    @Override
    public Observable<RxBleDeviceServices> discoverServices() {
        return getCachedDiscoveredServices()
                .flatMap(rxBleDeviceServices -> {
                    if (rxBleDeviceServices != null) {
                        return Observable.just(rxBleDeviceServices);
                    } else {
                        return actualDiscoverServices();
                    }
                });
    }

    private Observable<RxBleDeviceServices> getCachedDiscoveredServices() {
        final RxBleRadioOperationServicesDiscoverGetCached operationGetDiscoveredServices =
                new RxBleRadioOperationServicesDiscoverGetCached(discoveredServicesAtomicReference);
        final Observable<RxBleDeviceServices> observable = operationGetDiscoveredServices.asObservable();
        return observable.doOnSubscribe(() -> rxBleRadio.queue(operationGetDiscoveredServices));
    }

    private Observable<RxBleDeviceServices> actualDiscoverServices() {
        final RxBleRadioOperationServicesDiscover operationServicesDiscover =
                new RxBleRadioOperationServicesDiscover(gattCallback, bluetoothGattAtomicReference.get());
        final Observable<RxBleDeviceServices> observable = operationServicesDiscover.asObservable();
        return observable
                .doOnSubscribe(() -> rxBleRadio.queue(operationServicesDiscover))
                .doOnNext(discoveredServicesAtomicReference::set);
    }

    private Observable<BluetoothGattCharacteristic> getCharacteristic(UUID characteristicUuid) {
        return discoverServices()
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic(characteristicUuid));
    }

    public Observable<Observable<byte[]>> getNotification(UUID characteristicUuid) {
        return null; // TODO
    }

    public Observable<byte[]> readCharacteristic(UUID characteristicUuid) {
        return getCharacteristic(characteristicUuid)
                .flatMap(bluetoothGattCharacteristic -> {
                    final RxBleRadioOperationCharacteristicRead operationCharacteristicRead =
                            new RxBleRadioOperationCharacteristicRead(
                                    gattCallback,
                                    bluetoothGattAtomicReference.get(),
                                    bluetoothGattCharacteristic
                            );
                    final Observable<byte[]> observable = operationCharacteristicRead.asObservable();
                    return observable.doOnSubscribe(() -> rxBleRadio.queue(operationCharacteristicRead));
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
