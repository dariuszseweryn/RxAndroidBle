package com.polidea.rxandroidble;

import static com.polidea.rxandroidble.RxBleConnection.RxBleConnectionState.DISCONNECTED;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import com.polidea.rxandroidble.internal.RxBleRadio;
import java.util.concurrent.atomic.AtomicReference;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.subjects.BehaviorSubject;

public class RxBleDeviceImpl implements RxBleDevice {

    private final BluetoothDevice bluetoothDevice;

    private final RxBleRadio rxBleRadio;

    private final AtomicReference<Observable<RxBleConnection>> connectionObservable = new AtomicReference<>();

    private final BehaviorSubject<RxBleConnection.RxBleConnectionState> connectionStateBehaviorSubject = BehaviorSubject.create(
            DISCONNECTED
    );

    public RxBleDeviceImpl(BluetoothDevice bluetoothDevice, RxBleRadio rxBleRadio) {
        this.bluetoothDevice = bluetoothDevice;
        this.rxBleRadio = rxBleRadio;
    }

    public Observable<RxBleConnection.RxBleConnectionState> getConnectionState() {
        return connectionStateBehaviorSubject.distinctUntilChanged();
    }

    public Observable<RxBleConnection> establishConnection(Context context, boolean autoConnect) {
        synchronized (connectionObservable) {
            final Observable<RxBleConnection> rxBleConnectionObservable = connectionObservable.get();
            if (rxBleConnectionObservable != null) {
                return rxBleConnectionObservable;
            }

            final RxBleConnectionImpl rxBleConnection = new RxBleConnectionImpl(bluetoothDevice, rxBleRadio);
            final Subscription connectionStateSubscription = subscribeToConnectionStateChanges(rxBleConnection);

            final Observable<RxBleConnection> newConnectionObservable =
                    rxBleConnection
                            .connect(context, autoConnect)
                            .doOnSubscribe(() -> connectionStateBehaviorSubject.onNext(RxBleConnection.RxBleConnectionState.CONNECTING))
                            .doOnUnsubscribe(() -> {
                                synchronized (connectionObservable) {
                                    connectionObservable
                                            .set(null); //FIXME: [DS] 11.02.2016 Potential race condition when one subscriber would like to just after the previous one has unsubscribed
                                }
                                connectionStateBehaviorSubject.onNext(DISCONNECTED);
                                connectionStateSubscription.unsubscribe();
                            })
                            .replay()
                            .refCount();

            connectionObservable.set(newConnectionObservable);
            return newConnectionObservable;
        }
    }

    private Subscription subscribeToConnectionStateChanges(RxBleConnectionImpl rxBleConnection) {
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

    @Override
    public String getName() {
        return bluetoothDevice.getName();
    }

    @Override
    public String getMacAddress() {
        return bluetoothDevice.getAddress();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RxBleDeviceImpl)) {
            return false;
        }

        RxBleDeviceImpl that = (RxBleDeviceImpl) o;

        return bluetoothDevice.equals(that.bluetoothDevice);

    }

    @Override
    public int hashCode() {
        return bluetoothDevice.hashCode();
    }

    @Override
    public String toString() {
        return "RxBleDeviceImpl{" +
                "bluetoothDevice=" + bluetoothDevice.getName() + '(' + bluetoothDevice.getAddress() + ')' +
                '}';
    }
}
