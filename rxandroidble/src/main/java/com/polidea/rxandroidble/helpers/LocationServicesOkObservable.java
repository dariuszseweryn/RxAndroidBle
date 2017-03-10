package com.polidea.rxandroidble.helpers;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import com.polidea.rxandroidble.internal.util.CheckerLocationPermission;
import com.polidea.rxandroidble.internal.util.CheckerLocationProvider;
import com.polidea.rxandroidble.internal.util.LocationServicesStatus;
import com.polidea.rxandroidble.internal.util.ProviderApplicationTargetSdk;
import com.polidea.rxandroidble.internal.util.ProviderDeviceSdk;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import rx.Emitter;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Cancellable;
import rx.internal.operators.OnSubscribeCreate;

/**
 * An Observable that emits true when {@link com.polidea.rxandroidble.RxBleClient#scanBleDevices(UUID...)} would not
 * emit {@link com.polidea.rxandroidble.exceptions.BleScanException} with a reason
 * {@link com.polidea.rxandroidble.exceptions.BleScanException#LOCATION_SERVICES_DISABLED}
 */
public class LocationServicesOkObservable extends Observable<Boolean> {

    public static LocationServicesOkObservable createInstance(@NonNull final Context context) {
        final Context applicationContext = context.getApplicationContext();
        final LocationManager locationManager = (LocationManager) applicationContext.getSystemService(Context.LOCATION_SERVICE);
        final ProviderDeviceSdk providerDeviceSdk = new ProviderDeviceSdk();
        final ProviderApplicationTargetSdk providerApplicationTargetSdk = new ProviderApplicationTargetSdk(applicationContext);
        final CheckerLocationPermission checkerLocationPermission = new CheckerLocationPermission(applicationContext);
        final CheckerLocationProvider checkerLocationProvider = new CheckerLocationProvider(locationManager);
        final LocationServicesStatus locationServicesStatus = new LocationServicesStatus(
                checkerLocationProvider,
                checkerLocationPermission,
                providerDeviceSdk,
                providerApplicationTargetSdk
        );
        return new LocationServicesOkObservable(applicationContext, locationServicesStatus);
    }

    LocationServicesOkObservable(@NonNull final Context context, @NonNull final LocationServicesStatus locationServicesStatus) {
        super(new OnSubscribeCreate<>(
                new Action1<Emitter<Boolean>>() {
                    @Override
                    public void call(final Emitter<Boolean> emitter) {
                        final boolean locationProviderOk = locationServicesStatus.isLocationProviderOk();
                        final AtomicBoolean locationProviderOkAtomicBoolean = new AtomicBoolean(locationProviderOk);
                        emitter.onNext(locationProviderOk);

                        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                final boolean newLocationProviderOkValue = locationServicesStatus.isLocationProviderOk();
                                final boolean valueChanged = locationProviderOkAtomicBoolean
                                        .compareAndSet(!newLocationProviderOkValue, newLocationProviderOkValue);
                                if (valueChanged) {
                                    emitter.onNext(newLocationProviderOkValue);
                                }
                            }
                        };

                        context.registerReceiver(broadcastReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
                        emitter.setCancellation(new Cancellable() {
                            @Override
                            public void cancel() throws Exception {
                                context.unregisterReceiver(broadcastReceiver);
                            }
                        });
                    }
                },
                Emitter.BackpressureMode.LATEST
        ));
    }
}
