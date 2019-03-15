package com.polidea.rxandroidble2.internal.connection;


import com.jakewharton.rxrelay2.BehaviorRelay;
import com.polidea.rxandroidble2.RxBleAdapterStateObservable;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.exceptions.BleException;
import com.polidea.rxandroidble2.exceptions.BleGattException;
import com.polidea.rxandroidble2.internal.DeviceModule;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.internal.util.RxBleAdapterWrapper;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

/**
 * A class that is responsible for routing all potential sources of disconnection to an Observable that emits only errors.
 */
@ConnectionScope
class DisconnectionRouter implements DisconnectionRouterInput, DisconnectionRouterOutput {

    private final BehaviorRelay<BleException> bleExceptionBehaviorRelay = BehaviorRelay.create();
    private final Observable<BleException> firstDisconnectionValueObs;
    private final Observable<Object> firstDisconnectionExceptionObs;

    @Inject
    DisconnectionRouter(
            @Named(DeviceModule.MAC_ADDRESS) final String macAddress,
            final RxBleAdapterWrapper adapterWrapper,
            final Observable<RxBleAdapterStateObservable.BleAdapterState> adapterStateObservable
    ) {
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
        final Disposable adapterMonitoringDisposable = awaitAdapterNotUsable(adapterWrapper, adapterStateObservable)
                .map(new Function<Boolean, BleException>() {
                    @Override
                    public BleException apply(Boolean isAdapterUsable) {
                        return BleDisconnectedException.adapterDisabled(macAddress);
                    }
                })
                .doOnNext(new Consumer<BleException>() {
                    @Override
                    public void accept(BleException e) {
                        RxBleLog.d("An exception received, indicating that the adapter has became unusable.");
                    }
                })
                .subscribe(bleExceptionBehaviorRelay, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        RxBleLog.w(throwable, "Failed to monitor adapter state.");
                    }
                });

        firstDisconnectionValueObs = bleExceptionBehaviorRelay
                .firstElement()
                .toObservable()
                .doOnTerminate(new Action() {
                    @Override
                    public void run() {
                        adapterMonitoringDisposable.dispose();
                    }
                })
                .replay()
                .autoConnect(0);

        firstDisconnectionExceptionObs = firstDisconnectionValueObs
                .flatMap(new Function<BleException, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(BleException e) {
                        return Observable.error(e);
                    }
                });
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

    @Override
    public void onDisconnectedException(BleDisconnectedException disconnectedException) {
        bleExceptionBehaviorRelay.accept(disconnectedException);
    }

    @Override
    public void onGattConnectionStateException(BleGattException disconnectedGattException) {
        bleExceptionBehaviorRelay.accept(disconnectedGattException);
    }

    @Override
    public Observable<BleException> asValueOnlyObservable() {
        return firstDisconnectionValueObs;
    }

    @Override
    public <T> Observable<T> asErrorOnlyObservable() {
        // [DS 11.03.2019] Not an elegant solution but it should decrease amount of allocations. Should not emit values so —> safe to cast.
        return (Observable<T>) firstDisconnectionExceptionObs;
    }
}
