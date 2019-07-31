package com.polidea.rxandroidble2.internal.util;


import androidx.annotation.NonNull;

import com.polidea.rxandroidble2.ClientComponent;
import com.polidea.rxandroidble2.RxBleAdapterStateObservable;
import com.polidea.rxandroidble2.RxBleClient;

import java.util.concurrent.TimeUnit;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Predicate;

/**
 * The Observable class which emits changes to the Client State. These can be useful for evaluating if particular functionality
 * of the library has a chance to work properly.
 * <p>
 * For more info check {@link RxBleClient.State}
 */
public class ClientStateObservable extends Observable<RxBleClient.State> {

    final RxBleAdapterWrapper rxBleAdapterWrapper;
    final Observable<RxBleAdapterStateObservable.BleAdapterState> bleAdapterStateObservable;
    final Observable<Boolean> locationServicesOkObservable;
    private final LocationServicesStatus locationServicesStatus;
    private final Scheduler timerScheduler;

    @Inject
    protected ClientStateObservable(
            final RxBleAdapterWrapper rxBleAdapterWrapper,
            final Observable<RxBleAdapterStateObservable.BleAdapterState> bleAdapterStateObservable,
            @Named(ClientComponent.NamedBooleanObservables.LOCATION_SERVICES_OK) final Observable<Boolean> locationServicesOkObservable,
            final LocationServicesStatus locationServicesStatus,
            @Named(ClientComponent.NamedSchedulers.TIMEOUT) final Scheduler timerScheduler
    ) {
        this.rxBleAdapterWrapper = rxBleAdapterWrapper;
        this.bleAdapterStateObservable = bleAdapterStateObservable;
        this.locationServicesOkObservable = locationServicesOkObservable;
        this.locationServicesStatus = locationServicesStatus;
        this.timerScheduler = timerScheduler;
    }

    /**
     * Observable that emits `true` if the permission was granted on the time of subscription
     *
     * @param locationServicesStatus the LocationServicesStatus
     * @param timerScheduler         the Scheduler
     * @return the observable
     */
    @NonNull
    private static Single<Boolean> checkPermissionUntilGranted(
            final LocationServicesStatus locationServicesStatus,
            Scheduler timerScheduler
    ) {
        return Observable.interval(0, 1L, TimeUnit.SECONDS, timerScheduler)
                .takeWhile(new Predicate<Long>() {
                    @Override
                    public boolean test(Long timer) {
                        return !locationServicesStatus.isLocationPermissionOk();
                    }
                })
                .count()
                .map(new Function<Long, Boolean>() {
                    @Override
                    public Boolean apply(Long count) {
                        // if no elements were emitted then the permission was granted from the beginning
                        return count == 0;
                    }
                });
    }

    @NonNull
    static Observable<RxBleClient.State> checkAdapterAndServicesState(
            RxBleAdapterWrapper rxBleAdapterWrapper,
            Observable<RxBleAdapterStateObservable.BleAdapterState> rxBleAdapterStateObservable,
            final Observable<Boolean> locationServicesOkObservable
    ) {
        return rxBleAdapterStateObservable
                .startWithItem(rxBleAdapterWrapper.isBluetoothEnabled()
                        ? RxBleAdapterStateObservable.BleAdapterState.STATE_ON
                        /*
                         * Actual RxBleAdapterStateObservable.BleAdapterState does not really matter - because in the .switchMap() below
                         * we only check if it is STATE_ON or not
                         */
                        : RxBleAdapterStateObservable.BleAdapterState.STATE_OFF)
                .switchMap(new Function<RxBleAdapterStateObservable.BleAdapterState, Observable<RxBleClient.State>>() {
                    @Override
                    public Observable<RxBleClient.State> apply(
                            RxBleAdapterStateObservable.BleAdapterState bleAdapterState) {
                        if (bleAdapterState != RxBleAdapterStateObservable.BleAdapterState.STATE_ON) {
                            return Observable.just(RxBleClient.State.BLUETOOTH_NOT_ENABLED);
                        } else {
                            return locationServicesOkObservable.map(new Function<Boolean, RxBleClient.State>() {
                                @Override
                                public RxBleClient.State apply(Boolean locationServicesOk) {
                                    return locationServicesOk ? RxBleClient.State.READY
                                            : RxBleClient.State.LOCATION_SERVICES_NOT_ENABLED;
                                }
                            });
                        }
                    }
                });
    }

    @Override
    protected void subscribeActual(Observer<? super RxBleClient.State> observer) {
        if (!rxBleAdapterWrapper.hasBluetoothAdapter()) {
            observer.onSubscribe(Disposable.empty());
            observer.onComplete();
            return;
        }

        checkPermissionUntilGranted(locationServicesStatus, timerScheduler)
                .flatMapObservable(new Function<Boolean, Observable<RxBleClient.State>>() {
                    @Override
                    public Observable<RxBleClient.State> apply(Boolean permissionWasInitiallyGranted) {
                        Observable<RxBleClient.State> stateObservable = checkAdapterAndServicesState(
                                rxBleAdapterWrapper,
                                bleAdapterStateObservable,
                                locationServicesOkObservable
                        )
                                .distinctUntilChanged();
                        return permissionWasInitiallyGranted
                                /*
                                 * If permission was granted from the beginning then the first value is not a change. The above Observable
                                 * does emit value at the moment of subscription.
                                 */
                                ? stateObservable.skip(1)
                                : stateObservable;
                    }
                })
                .subscribe(observer);
    }
}
