package com.polidea.rxandroidble.internal.connection;


import com.jakewharton.rxrelay.PublishRelay;
import com.polidea.rxandroidble.RxBleAdapterStateObservable;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleGattException;
import com.polidea.rxandroidble.internal.DeviceModule;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.functions.Func1;

/**
 * A class that is responsible for routing all potential sources of disconnection to an Observable that emits only errors.
 */
@ConnectionScope
class DisconnectionRouter {

    private final PublishRelay<BleException> disconnectionErrorRelay = PublishRelay.create();

    private final Observable disconnectionErrorObservable;

    @Inject
    DisconnectionRouter(
            @Named(DeviceModule.MAC_ADDRESS) final String macAddress,
            final RxBleAdapterWrapper adapterWrapper,
            final Observable<RxBleAdapterStateObservable.BleAdapterState> adapterStateObservable
    ) {
        disconnectionErrorObservable = Observable.merge(
                disconnectionErrorRelay
                        .flatMap(new Func1<BleException, Observable<?>>() {
                            @Override
                            public Observable<?> call(BleException e) {
                                return Observable.error(e);
                            }
                        }),
                adapterStateObservable
                        .map(new Func1<RxBleAdapterStateObservable.BleAdapterState, Boolean>() {
                            @Override
                            public Boolean call(RxBleAdapterStateObservable.BleAdapterState bleAdapterState) {
                                return bleAdapterState.isUsable();
                            }
                        })
                        .startWith(adapterWrapper.isBluetoothEnabled())
                        .filter(new Func1<Boolean, Boolean>() {
                            @Override
                            public Boolean call(Boolean isAdapterUsable) {
                                return !isAdapterUsable;
                            }
                        })
                        .flatMap(new Func1<Boolean, Observable<?>>() {
                            @Override
                            public Observable<?> call(Boolean isAdapterUsable) {
                                return Observable.error(new BleDisconnectedException(macAddress)); // TODO: Introduce BleDisabledException?
                            }
                        })
        )
                .replay()
                .autoConnect(0);
    }

    /**
     * Method to be called whenever a connection braking exception happens. It will be routed to {@link #asObservable()}.
     *
     * @param disconnectedException the exception that happened
     */
    void onDisconnectedException(BleDisconnectedException disconnectedException) {
        disconnectionErrorRelay.call(disconnectedException);
    }

    /**
     * Method to be called whenever a BluetoothGattCallback.onConnectionStateChange() will get called with status != GATT_SUCCESS
     *
     * @param disconnectedGattException the exception that happened
     */
    void onGattConnectionStateException(BleGattException disconnectedGattException) {
        disconnectionErrorRelay.call(disconnectedGattException);
    }

    /**
     * Function returning an Observable that will only throw error in case of a disconnection
     *
     * @param <T> the type of returned observable
     * @return the Observable
     */
    <T> Observable<T> asObservable() {
        //noinspection unchecked
        return disconnectionErrorObservable;
    }
}
