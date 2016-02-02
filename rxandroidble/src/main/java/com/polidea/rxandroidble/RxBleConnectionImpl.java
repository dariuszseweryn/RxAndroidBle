package com.polidea.rxandroidble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.polidea.rxandroidble.internal.RxBleConnectibleConnection;
import com.polidea.rxandroidble.internal.RxBleGattCallback;
import com.polidea.rxandroidble.internal.RxBleRadio;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationCharacteristicRead;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationCharacteristicWrite;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationConnect;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDescriptorRead;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDescriptorWrite;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationDisconnect;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationServicesDiscoverGetCached;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationReadRssi;
import com.polidea.rxandroidble.internal.operations.RxBleRadioOperationServicesDiscover;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import rx.Observable;

public class RxBleConnectionImpl implements RxBleConnectibleConnection {

    private final BluetoothDevice bluetoothDevice;

    private final RxBleRadio rxBleRadio;

    private RxBleGattCallback gattCallback = new RxBleGattCallback();

    private final AtomicReference<BluetoothGatt> bluetoothGattAtomicReference = new AtomicReference<>();

    private AtomicReference<RxBleDeviceServices> discoveredServicesAtomicReference = new AtomicReference<>();

    public RxBleConnectionImpl(BluetoothDevice bluetoothDevice, RxBleRadio rxBleRadio) {

        this.bluetoothDevice = bluetoothDevice;
        this.rxBleRadio = rxBleRadio;
    }

    @Override
    public Observable<RxBleConnection> connect(Context context) {
        final RxBleRadioOperationConnect operationConnect = new RxBleRadioOperationConnect(context, bluetoothDevice, gattCallback, this);
        final RxBleRadioOperationDisconnect operationDisconnect = new RxBleRadioOperationDisconnect(
                gattCallback,
                bluetoothGattAtomicReference,
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)
        );
        // TODO: [PU] 29.01.2016 Will crash if onError will be passed through the subject.
        operationConnect.getBluetoothGatt().subscribe(bluetoothGattAtomicReference::set);
        final Observable<RxBleConnection> observable = operationConnect.asObservable();
        return observable
                .doOnSubscribe(() -> rxBleRadio.queue(operationConnect))
                .doOnError(throwable -> rxBleRadio.queue(operationDisconnect))
                .doOnUnsubscribe(() -> rxBleRadio.queue(operationDisconnect));
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

    @Override
    public Observable<Observable<byte[]>> getNotification(UUID characteristicUuid) {
        return null; // TODO
    }

    @Override
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

    @Override
    public Observable<byte[]> writeCharacteristic(UUID characteristicUuid, byte[] data) {
        return getCharacteristic(characteristicUuid)
                .flatMap(bluetoothGattCharacteristic -> {
                    final RxBleRadioOperationCharacteristicWrite operationCharacteristicWrite = new RxBleRadioOperationCharacteristicWrite(
                            gattCallback,
                            bluetoothGattAtomicReference.get(),
                            bluetoothGattCharacteristic,
                            data
                    );

                    final Observable<byte[]> observable = operationCharacteristicWrite.asObservable();
                    return observable.doOnSubscribe(() -> rxBleRadio.queue(operationCharacteristicWrite));
                });
    }

    @Override
    public Observable<byte[]> readDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid) {
        return discoverServices()
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getDescriptor(serviceUuid, characteristicUuid, descriptorUuid))
                .flatMap(bluetoothGattDescriptor -> {
                    final RxBleRadioOperationDescriptorRead operationDescriptorRead =
                            new RxBleRadioOperationDescriptorRead(gattCallback, bluetoothGattAtomicReference.get(), bluetoothGattDescriptor);
                    final Observable<byte[]> observable = operationDescriptorRead.asObservable();
                    return observable.doOnSubscribe(() -> rxBleRadio.queue(operationDescriptorRead));
                });
    }

    @Override
    public Observable<byte[]> writeDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, byte[] data) {
        return discoverServices()
                .flatMap(rxBleDeviceServices -> rxBleDeviceServices.getDescriptor(serviceUuid, characteristicUuid, descriptorUuid))
                .flatMap(bluetoothGattDescriptor -> {
                    final RxBleRadioOperationDescriptorWrite operationDescriptorWrite =
                            new RxBleRadioOperationDescriptorWrite(gattCallback, bluetoothGattAtomicReference.get(), bluetoothGattDescriptor, data);
                    final Observable<byte[]> observable = operationDescriptorWrite.asObservable();
                    return observable.doOnSubscribe(() -> rxBleRadio.queue(operationDescriptorWrite));
                });
    }

    @Override
    public Observable<Integer> readRssi() {
        final RxBleRadioOperationReadRssi operationReadRssi = new RxBleRadioOperationReadRssi(gattCallback, bluetoothGattAtomicReference.get());
        final Observable<Integer> observable = operationReadRssi.asObservable();
        return observable.doOnSubscribe(() -> rxBleRadio.queue(operationReadRssi));
    }
}
