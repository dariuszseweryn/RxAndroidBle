package com.polidea.rxandroidble.mockrxandroidble;

import android.bluetooth.BluetoothDevice;
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
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.subjects.BehaviorSubject;

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTED;
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTING;
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.DISCONNECTED;

public class RxBleDeviceMock implements RxBleDevice {

    private RxBleConnection rxBleConnection;
    private BehaviorSubject<RxBleConnection.RxBleConnectionState> connectionStateBehaviorSubject = BehaviorSubject.create(
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
        return Observable.defer(new Func0<Observable<RxBleConnection>>() {
            @Override
            public Observable<RxBleConnection> call() {
                if (isConnected.compareAndSet(false, true)) {
                    return RxBleDeviceMock.this.emitConnectionWithoutCompleting()
                            .doOnSubscribe(new Action0() {
                                @Override
                                public void call() {
                                    connectionStateBehaviorSubject.onNext(CONNECTING);
                                }
                            })
                            .doOnNext(new Action1<RxBleConnection>() {
                                @Override
                                public void call(RxBleConnection rxBleConnection) {
                                    connectionStateBehaviorSubject.onNext(CONNECTED);
                                }
                            })
                            .doOnUnsubscribe(new Action0() {
                                @Override
                                public void call() {
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

    private Observable<RxBleConnection> emitConnectionWithoutCompleting() {
        return Observable.<RxBleConnection>never().startWith(rxBleConnection);
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
    public BluetoothDevice getBluetoothDevice() {
        throw new UnsupportedOperationException("Mock does not support returning a "
            + "BluetoothDevice.");
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
