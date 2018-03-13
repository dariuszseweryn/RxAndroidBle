package com.polidea.rxandroidble2.mockrxandroidble;

import android.bluetooth.BluetoothDevice;

import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.Timeout;
import com.polidea.rxandroidble2.exceptions.BleAlreadyConnectedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;

import static com.polidea.rxandroidble2.RxBleConnection.RxBleConnectionState.CONNECTED;
import static com.polidea.rxandroidble2.RxBleConnection.RxBleConnectionState.CONNECTING;
import static com.polidea.rxandroidble2.RxBleConnection.RxBleConnectionState.DISCONNECTED;

public class RxBleDeviceMock implements RxBleDevice {

    private RxBleConnection rxBleConnection;
    private BehaviorSubject<RxBleConnection.RxBleConnectionState> connectionStateBehaviorSubject = BehaviorSubject.createDefault(
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
    public Observable<RxBleConnection> establishConnection(boolean autoConnect) {
        return Observable.defer(new Callable<Observable<RxBleConnection>>() {
            @Override
            public Observable<RxBleConnection> call() {
                if (isConnected.compareAndSet(false, true)) {
                    return RxBleDeviceMock.this.emitConnectionWithoutCompleting()
                            .doOnSubscribe(new Consumer<Disposable>() {
                                @Override
                                public void accept(Disposable disposable) throws Exception {
                                    connectionStateBehaviorSubject.onNext(CONNECTING);
                                }
                            })
                            .doOnNext(new Consumer<RxBleConnection>() {
                                @Override
                                public void accept(RxBleConnection rxBleConnection) throws Exception {
                                    connectionStateBehaviorSubject.onNext(CONNECTED);
                                }
                            })
                            .doFinally(new Action() {
                                @Override
                                public void run() {
                                    connectionStateBehaviorSubject.onNext(DISCONNECTED);
                                    isConnected.set(false);
                                }
                            });
                } else {
                    return Observable.error(new BleAlreadyConnectedException(macAddress));
                }
            }
        });
    }

    @Override
    public Observable<RxBleConnection> establishConnection(boolean autoConnect, Timeout operationTimeout) {
        return establishConnection(autoConnect);
    }

    private Observable<RxBleConnection> emitConnectionWithoutCompleting() {
        return Observable.<RxBleConnection>never().startWith(rxBleConnection);
    }

    public List<UUID> getAdvertisedUUIDs() {
        return advertisedUUIDs;
    }

    @Override
    public RxBleConnection.RxBleConnectionState getConnectionState() {
        return observeConnectionStateChanges().blockingFirst();
    }

    @Override
    public String getMacAddress() {
        return macAddress;
    }

    @Override
    public BluetoothDevice getBluetoothDevice() {
        throw new UnsupportedOperationException("Mock does not support returning a BluetoothDevice.");
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
