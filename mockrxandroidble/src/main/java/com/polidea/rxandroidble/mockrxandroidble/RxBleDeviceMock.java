package com.polidea.rxandroidble.mockrxandroidble;

import android.content.Context;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleDeviceServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.subjects.BehaviorSubject;

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTED;
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.CONNECTING;
import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.DISCONNECTED;

class RxBleDeviceMock implements RxBleDevice {

    private final RxBleConnection.Connector connector;
    private final BehaviorSubject<RxBleConnection.RxBleConnectionState> connectionStateBehaviorSubject = BehaviorSubject.create(
            DISCONNECTED
    );
    private String name;
    private String macAddress;
    private Integer rssi;
    private byte[] scanRecord;
    private List<UUID> advertisedUUIDs;

    public RxBleDeviceMock(String name, String macAddress, byte[] scanRecord, Integer rssi, RxBleDeviceServices rxBleDeviceServices, Map<UUID, Observable<byte[]>> characteristicNotificationSources) {
        this.name = name;
        this.macAddress = macAddress;
        this.connector = new RxBleConnectionConnectorMock(new RxBleConnectionMock(rxBleDeviceServices, rssi, characteristicNotificationSources, connectionStateBehaviorSubject));
        this.rssi = rssi;
        this.scanRecord = scanRecord;
        this.advertisedUUIDs = new ArrayList<>();
    }

    @Override
    public Observable<RxBleConnection> establishConnection(Context context, boolean autoConnect) {
        return Observable.defer(() -> {

            final AtomicReference<Subscription> connectionStateSubscription = new AtomicReference<>();

            return connector.prepareConnection(context, autoConnect)
                    .doOnSubscribe(() -> connectionStateBehaviorSubject.onNext(CONNECTING))
                    .doOnNext(rxBleConnection -> {
                        connectionStateBehaviorSubject.onNext(CONNECTED);
                        connectionStateSubscription.set(subscribeToConnectionStateChanges(rxBleConnection));
                    })
                    .doOnUnsubscribe(() -> {
                        connectionStateBehaviorSubject.onNext(DISCONNECTED);
                        final Subscription subscription = connectionStateSubscription.get();
                        if (subscription != null) {
                            subscription.unsubscribe();
                        }
                    })
                    .replay()
                    .refCount();
        });
    }

    @Override
    public Observable<RxBleConnection.RxBleConnectionState> getConnectionState() {
        return connectionStateBehaviorSubject.distinctUntilChanged();
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

    @Override
    public String toString() {
        return "RxBleDeviceImpl{" +
                "bluetoothDevice=" + name + '(' + macAddress + ')' +
                '}';
    }

    public List<UUID> getAdvertisedUUIDs() {
        return advertisedUUIDs;
    }

    public byte[] getScanRecord() {
        return scanRecord;
    }

    private Subscription subscribeToConnectionStateChanges(RxBleConnection rxBleConnection) {
        return rxBleConnection
                .getConnectionState()
                .<RxBleConnection.RxBleConnectionState>lift(subscriber -> new Subscriber<RxBleConnection.RxBleConnectionState>() {
                    @Override
                    public void onCompleted() {
                        // do nothing so the connectionStateBehaviorSubject will not complete
                    }

                    @Override
                    public void onError(Throwable e) {
                        // same as above -
                        subscriber.onNext(DISCONNECTED);
                    }

                    @Override
                    public void onNext(RxBleConnection.RxBleConnectionState rxBleConnectionState) {
                        subscriber.onNext(rxBleConnectionState);
                    }
                })
                .subscribe(connectionStateBehaviorSubject);
    }
}
