package com.polidea.rxandroidble.mockrxandroidble;

import android.content.Context;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleAlreadyConnectedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.subjects.BehaviorSubject;

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTED;
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTING;
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.DISCONNECTED;

class RxBleDeviceMock implements RxBleDevice {

    private final RxBleConnection rxBleConnection;
    private final BehaviorSubject<RxBleConnection.RxBleConnectionState> connectionStateBehaviorSubject = BehaviorSubject.create(
            DISCONNECTED
    );
    private String name;
    private String macAddress;
    private Integer rssi;
    private byte[] scanRecord;
    private List<UUID> advertisedUUIDs;
    private AtomicBoolean isConnected = new AtomicBoolean(false);

    public RxBleDeviceMock(String name,
                           String macAddress,
                           byte[] scanRecord,
                           Integer rssi,
                           RxBleDeviceServices rxBleDeviceServices,
                           Map<UUID, Observable<byte[]>> characteristicNotificationSources) {
        this.name = name;
        this.macAddress = macAddress;
        this.rxBleConnection = new RxBleConnectionMock(rxBleDeviceServices,
                rssi,
                characteristicNotificationSources);
        this.rssi = rssi;
        this.scanRecord = scanRecord;
        this.advertisedUUIDs = new ArrayList<>();
    }

    public void addAdvertisedUUID(UUID advertisedUUID) {
        advertisedUUIDs.add(advertisedUUID);
    }

    @Override
    public Observable<RxBleConnection> establishConnection(Context context, boolean autoConnect) {
        return Observable.defer(() -> {
            if (isConnected.compareAndSet(false, true)) {
                return Observable.just(rxBleConnection)
                        .doOnSubscribe(() -> connectionStateBehaviorSubject.onNext(CONNECTING))
                        .doOnNext(rxBleConnection -> connectionStateBehaviorSubject.onNext(CONNECTED))
                        .doOnUnsubscribe(() -> {
                            connectionStateBehaviorSubject.onNext(DISCONNECTED);
                            isConnected.set(false);
                        });
            } else {
                return Observable.error(new BleAlreadyConnectedException(macAddress));
            }
        });
    }

    public List<UUID> getAdvertisedUUIDs() {
        return advertisedUUIDs;
    }

    @Override
    public RxBleConnection.RxBleConnectionState getConnectionState() {
        return observeConnectionStateChanges().toBlocking().first();
    }

    @Override
    public String getMacAddress() {
        return macAddress;
    }

    @Override
    public String getName() {
        return name;
    }

    public Integer getRssi() {
        return rssi;
    }

    public byte[] getScanRecord() {
        return scanRecord;
    }

    @Override
    public Observable<RxBleConnection.RxBleConnectionState> observeConnectionStateChanges() {
        return connectionStateBehaviorSubject.distinctUntilChanged();
    }

    @Override
    public String toString() {
        return "RxBleDeviceImpl{" + "bluetoothDevice=" + name + '(' + macAddress + ')' + '}';

    }
}
