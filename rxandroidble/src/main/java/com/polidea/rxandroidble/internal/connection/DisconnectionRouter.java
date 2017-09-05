package com.polidea.rxandroidble.internal.connection;


import com.jakewharton.rxrelay.BehaviorRelay;
import com.polidea.rxandroidble.RxBleAdapterStateObservable;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.internal.DeviceModule;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Observable;
import rx.functions.Func1;

/**
 * A class that is responsible for routing all potential sources of disconnection to an Observable that emits only errors.
 */
@ConnectionScope
class DisconnectionRouter {

    private final BehaviorRelay<BleException> disconnectionErrorRelay = BehaviorRelay.create();

    private final Observable disconnectionErrorObservable;

    @Inject
    DisconnectionRouter(
            @Named(DeviceModule.MAC_ADDRESS) final String macAddress,
            Observable<RxBleAdapterStateObservable.BleAdapterState> adapterStateObservable
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
                        .filter(new Func1<RxBleAdapterStateObservable.BleAdapterState, Boolean>() {
                            @Override
                            public Boolean call(RxBleAdapterStateObservable.BleAdapterState bleAdapterState) {
                                return !bleAdapterState.isUsable();
                            }
                        })
                        .flatMap(new Func1<RxBleAdapterStateObservable.BleAdapterState, Observable<?>>() {
                            @Override
                            public Observable<?> call(RxBleAdapterStateObservable.BleAdapterState bleAdapterState) {
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
     * @param bleException the exception that happened
     */
    void route(BleException bleException) {
        disconnectionErrorRelay.call(bleException);
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
