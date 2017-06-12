package com.polidea.rxandroidble.internal.util;


import android.support.annotation.NonNull;
import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.RxBleAdapterStateObservable;
import com.polidea.rxandroidble.RxBleClient;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import rx.Emitter;
import rx.Observable;
import rx.Scheduler;
import rx.Single;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Cancellable;
import rx.functions.Func1;
import rx.internal.operators.OnSubscribeCreate;

/**
 * The Observable class which emits changes to the Client State. These can be useful for evaluating if particular functionality
 * of the library has a chance to work properly.
 *
 * For more info check {@link RxBleClient.State}
 */
public class ClientStateObservable extends Observable<RxBleClient.State> {

    @Inject
    protected ClientStateObservable(
            final RxBleAdapterWrapper rxBleAdapterWrapper,
            final Observable<RxBleAdapterStateObservable.BleAdapterState> bleAdapterStateObservable,
            @Named(ClientComponent.NamedBooleanObservables.LOCATION_SERVICES_OK) final Observable<Boolean> locationServicesOkObservable,
            final LocationServicesStatus locationServicesStatus,
            @Named(ClientComponent.NamedSchedulers.TIMEOUT) final Scheduler timerScheduler
    ) {
        super(new OnSubscribeCreate<>(
                new Action1<Emitter<RxBleClient.State>>() {
                    @Override
                    public void call(Emitter<RxBleClient.State> emitter) {
                        if (!rxBleAdapterWrapper.hasBluetoothAdapter()) {
                            emitter.onCompleted();
                            return;
                        }

                        final Subscription changingStateSubscription = checkPermissionUntilGranted(locationServicesStatus, timerScheduler)
                                .flatMapObservable(new Func1<Boolean, Observable<RxBleClient.State>>() {
                                    @Override
                                    public Observable<RxBleClient.State> call(Boolean permissionWasInitiallyGranted) {
                                        return checkAdapterAndServicesState(
                                                permissionWasInitiallyGranted,
                                                rxBleAdapterWrapper,
                                                bleAdapterStateObservable,
                                                locationServicesOkObservable
                                        );
                                    }
                                })
                                .distinctUntilChanged()
                                .subscribe(emitter);

                        emitter.setCancellation(new Cancellable() {
                            @Override
                            public void cancel() throws Exception {
                                changingStateSubscription.unsubscribe();
                            }
                        });
                    }
                },
                Emitter.BackpressureMode.LATEST
        ));
    }

    /**
     * Observable that emits `true` if the permission was granted on the time of subscription
     * @param locationServicesStatus the LocationServicesStatus
     * @param timerScheduler the Scheduler
     * @return the observable
     */
    @NonNull
    private static Single<Boolean> checkPermissionUntilGranted(
            final LocationServicesStatus locationServicesStatus,
            Scheduler timerScheduler
    ) {
        return Observable.interval(0, 1L, TimeUnit.SECONDS, timerScheduler)
                .map(new Func1<Long, Boolean>() {
                    @Override
                    public Boolean call(Long aLong) {
                        return locationServicesStatus.isLocationPermissionOk();
                    }
                })
                .takeWhile(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean aBoolean) {
                        return !aBoolean;
                    }
                })
                .count()
                .toSingle()
                .map(new Func1<Integer, Boolean>() {
                    @Override
                    public Boolean call(Integer integer) {
                        // if no elements were emitted then the permission was granted from the beginning
                        return integer == 0;
                    }
                });
    }

    @NonNull
    private static Observable<RxBleClient.State> checkAdapterAndServicesState(
            Boolean permissionWasInitiallyGranted,
            RxBleAdapterWrapper rxBleAdapterWrapper,
            Observable<RxBleAdapterStateObservable.BleAdapterState> rxBleAdapterStateObservable,
            final Observable<Boolean> locationServicesOkObservable
    ) {
        final Observable<RxBleClient.State> stateObservable = rxBleAdapterStateObservable
                .startWith(rxBleAdapterWrapper.isBluetoothEnabled()
                        ? RxBleAdapterStateObservable.BleAdapterState.STATE_ON
                        /*
                        Actual RxBleAdapterStateObservable.BleAdapterState does not really matter - because in the .switchMap() below
                        we only check if it is STATE_ON or not
                         */
                        : RxBleAdapterStateObservable.BleAdapterState.STATE_OFF)
                .switchMap(new Func1<RxBleAdapterStateObservable.BleAdapterState, Observable<RxBleClient.State>>() {
                    @Override
                    public Observable<RxBleClient.State> call(
                            RxBleAdapterStateObservable.BleAdapterState bleAdapterState) {
                        if (bleAdapterState != RxBleAdapterStateObservable.BleAdapterState.STATE_ON) {
                            return Observable.just(RxBleClient.State.BLUETOOTH_NOT_ENABLED);
                        } else {
                            return locationServicesOkObservable.map(new Func1<Boolean, RxBleClient.State>() {
                                @Override
                                public RxBleClient.State call(Boolean locationServicesOk) {
                                    return locationServicesOk ? RxBleClient.State.READY
                                            : RxBleClient.State.LOCATION_SERVICES_NOT_ENABLED;
                                }
                            });
                        }
                    }
                });
        return permissionWasInitiallyGranted
                /*
                If permission was granted from the beginning then the first value is not a change as the above Observable does emit value
                at the moment of Subscription.
                 */
                ? stateObservable.skip(1)
                : stateObservable;
    }
}
