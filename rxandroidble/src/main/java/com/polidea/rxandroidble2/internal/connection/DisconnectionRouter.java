package com.polidea.rxandroidble2.internal.connection;


import com.jakewharton.rxrelay2.PublishRelay;
import com.polidea.rxandroidble2.RxBleAdapterStateObservable;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.exceptions.BleException;
import com.polidea.rxandroidble2.exceptions.BleGattException;
import com.polidea.rxandroidble2.internal.DeviceModule;
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

/**
 * A class that is responsible for routing all potential sources of disconnection to an Observable that emits only errors.
 */
@ConnectionScope
class DisconnectionRouter implements DisconnectionRouterInput, DisconnectionRouterOutput {

    private final PublishRelay<BleException> disconnectionErrorInputRelay = PublishRelay.create();
    private final Observable<BleException> disconnectionErrorOutputObservable;

    @Inject
    DisconnectionRouter(
            @Named(DeviceModule.MAC_ADDRESS) final String macAddress,
            final RxBleAdapterWrapper adapterWrapper,
            final Observable<RxBleAdapterStateObservable.BleAdapterState> adapterStateObservable
    ) {
        final Observable<BleException> emitErrorWhenAdapterIsDisabled = awaitAdapterNotUsable(adapterWrapper, adapterStateObservable)
                .map(new Function<Boolean, BleException>() {
                    @Override
                    public BleException apply(Boolean isAdapterUsable) {
                        return new BleDisconnectedException(macAddress); // TODO: Introduce BleDisabledException?
                    }
                });

        disconnectionErrorOutputObservable = Observable.merge(
                disconnectionErrorInputRelay,
                emitErrorWhenAdapterIsDisabled
        )
                .firstOrError() // to unsubscribe from adapterStateObservable on first emission
                .toObservable()
                .cache();

        /*
         The below .subscribe() is only to make the above .cache() to start working as soon as possible.
         We are not tracking the resulting `Subscription`. This is because of the contract of this class which is supposed to be called
         when a disconnection happens from one of three places:
            1. adapterStateObservable: the adapter turning into state other than STATE_ON
            2. onDisconnectedException
            3. onGattConnectionStateException
         One of those events must happen eventually. Then the adapterStateObservable (which uses BroadcastReceiver on a Context) will
         get unsubscribed. The rest of this chain lives only in the @ConnectionScope context and will get Garbage Collected eventually.
         */
        disconnectionErrorOutputObservable.subscribe();
    }

    private static Observable<Boolean> awaitAdapterNotUsable(RxBleAdapterWrapper adapterWrapper,
                                                         Observable<RxBleAdapterStateObservable.BleAdapterState> stateChanges) {
        return stateChanges
                .map(new Function<RxBleAdapterStateObservable.BleAdapterState, Boolean>() {
                    @Override
                    public Boolean apply(RxBleAdapterStateObservable.BleAdapterState bleAdapterState) {
                        return bleAdapterState.isUsable();
                    }
                })
                .startWith(adapterWrapper.isBluetoothEnabled())
                .filter(new Predicate<Boolean>() {
                    @Override
                    public boolean test(Boolean isAdapterUsable) {
                        return !isAdapterUsable;
                    }
                });
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onDisconnectedException(BleDisconnectedException disconnectedException) {
        disconnectionErrorInputRelay.accept(disconnectedException);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onGattConnectionStateException(BleGattException disconnectedGattException) {
        disconnectionErrorInputRelay.accept(disconnectedGattException);
    }

    /**
     * @inheritDoc
     */
    @Override
    public Observable<BleException> asValueOnlyObservable() {
        return disconnectionErrorOutputObservable;
    }

    /**
     * @inheritDoc
     */
    @Override
    public <T> Observable<T> asErrorOnlyObservable() {
        return disconnectionErrorOutputObservable
                .flatMap(new Function<BleException, Observable<T>>() {
                    @Override
                    public Observable<T> apply(BleException e) {
                        return Observable.error(e);
                    }
                });
    }
}
