package com.polidea.rxandroidble.internal.connection;


import com.jakewharton.rxrelay.PublishRelay;
import com.polidea.rxandroidble.RxBleAdapterStateObservable;
import com.polidea.rxandroidble.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble.exceptions.BleException;
import com.polidea.rxandroidble.exceptions.BleGattException;
import com.polidea.rxandroidble.internal.DeviceModule;
import com.polidea.rxandroidble.internal.util.RxBleAdapterWrapper;
import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import rx.Observable;
import rx.functions.Func1;

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
        final Observable<BleException> emitErrorWhenAdapterIsDisabled = adapterStateObservable
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
                .map(new Func1<Boolean, BleException>() {
                    @Override
                    public BleException call(Boolean isAdapterUsable) {
                        return new BleDisconnectedException(macAddress); // TODO: Introduce BleDisabledException?
                    }
                });

        disconnectionErrorOutputObservable = Observable.merge(
                disconnectionErrorInputRelay,
                emitErrorWhenAdapterIsDisabled
        )
                .replay()
                .autoConnect(0);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onDisconnectedException(BleDisconnectedException disconnectedException) {
        disconnectionErrorInputRelay.call(disconnectedException);
    }

    /**
     * @inheritDoc
     */
    @Override
    public void onGattConnectionStateException(BleGattException disconnectedGattException) {
        disconnectionErrorInputRelay.call(disconnectedGattException);
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
        return disconnectionErrorOutputObservable.flatMap(new Func1<BleException, Observable<T>>() {
            @Override
            public Observable<T> call(BleException e) {
                return Observable.error(e);
            }
        });
    }
}
