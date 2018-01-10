package com.polidea.rxandroidble.internal.util;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Build;
import java.util.concurrent.atomic.AtomicBoolean;
import rx.Emitter;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Cancellable;
import rx.internal.operators.OnSubscribeCreate;
import bleshadow.javax.inject.Inject;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class LocationServicesOkObservableApi23 extends Observable<Boolean> {

    @Inject
    LocationServicesOkObservableApi23(
            final Context context,
            final LocationServicesStatus locationServicesStatus
    ) {
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

                        context.registerReceiver(broadcastReceiver, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
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
