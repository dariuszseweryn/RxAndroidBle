package com.polidea.rxandroidble.helpers;


import android.content.Context;
import android.support.annotation.NonNull;

import com.polidea.rxandroidble.ClientComponent;
import com.polidea.rxandroidble.DaggerClientComponent;

import java.util.UUID;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import rx.Emitter;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.internal.operators.OnSubscribeCreate;

/**
 * An Observable that emits false if an attempt to scan with {@link com.polidea.rxandroidble.RxBleClient#scanBleDevices(UUID...)}
 * would cause the exception {@link com.polidea.rxandroidble.exceptions.BleScanException#LOCATION_SERVICES_DISABLED}; otherwise emits true.
 * Always emits true in Android versions prior to 6.0.
 * Typically, receiving false should cause the user to be prompted to enable Location Services.
 */
public class LocationServicesOkObservable extends Observable<Boolean> {

    public static LocationServicesOkObservable createInstance(@NonNull final Context context) {
        return DaggerClientComponent
                .builder()
                .clientModule(new ClientComponent.ClientModule(context, null))
                .build()
                .locationServicesOkObservable();
    }

    @Inject
    LocationServicesOkObservable(
            @Named(ClientComponent.NamedBooleanObservables.LOCATION_SERVICES_OK) final Observable<Boolean> locationServicesOkObsImpl
    ) {
        super(new OnSubscribeCreate<>(
                new Action1<Emitter<Boolean>>() {
                    @Override
                    public void call(final Emitter<Boolean> emitter) {
                        Subscription subscription = locationServicesOkObsImpl.subscribe(emitter);
                        emitter.setSubscription(subscription);
                    }
                },
                Emitter.BackpressureMode.LATEST
        ));
    }
}
