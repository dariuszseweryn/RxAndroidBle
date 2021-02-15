package com.polidea.rxandroidble3.helpers;

import android.content.Context;
import androidx.annotation.NonNull;

import com.polidea.rxandroidble3.ClientComponent;
import com.polidea.rxandroidble3.DaggerClientComponent;

import java.util.UUID;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;

/**
 * An Observable that emits false if an attempt to scan with {@link com.polidea.rxandroidble3.RxBleClient#scanBleDevices(UUID...)}
 * would cause the exception {@link com.polidea.rxandroidble3.exceptions.BleScanException#LOCATION_SERVICES_DISABLED}; otherwise emits true.
 * Always emits true in Android versions prior to 6.0.
 * Typically, receiving false should cause the user to be prompted to enable Location Services.
 */
public class LocationServicesOkObservable extends Observable<Boolean> {

    @NonNull
    private final Observable<Boolean> locationServicesOkObsImpl;

    public static LocationServicesOkObservable createInstance(@NonNull final Context context) {
        return DaggerClientComponent
                .builder()
                .applicationContext(context.getApplicationContext())
                .build()
                .locationServicesOkObservable();
    }

    @Inject
    LocationServicesOkObservable(
            @NonNull
            @Named(ClientComponent.NamedBooleanObservables.LOCATION_SERVICES_OK) final Observable<Boolean> locationServicesOkObsImpl) {
        this.locationServicesOkObsImpl = locationServicesOkObsImpl;
    }

    @Override
    protected void subscribeActual(final Observer<? super Boolean> observer) {
        locationServicesOkObsImpl.subscribe(observer);
    }
}
