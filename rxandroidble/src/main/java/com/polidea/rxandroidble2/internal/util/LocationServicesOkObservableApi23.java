package com.polidea.rxandroidble2.internal.util;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Build;

import java.util.concurrent.atomic.AtomicBoolean;

import bleshadow.javax.inject.Inject;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Action;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class LocationServicesOkObservableApi23 extends Observable<Boolean> {

    private final Context context;
    private final LocationServicesStatus locationServicesStatus;

    @Inject
    LocationServicesOkObservableApi23(final Context context, final LocationServicesStatus locationServicesStatus) {
        this.context = context;
        this.locationServicesStatus = locationServicesStatus;
    }

    @Override
    protected void subscribeActual(final Observer<? super Boolean> observer) {
        final boolean locationProviderOk = locationServicesStatus.isLocationProviderOk();
        final AtomicBoolean locationProviderOkAtomicBoolean = new AtomicBoolean(locationProviderOk);
        final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final boolean newLocationProviderOkValue = locationServicesStatus.isLocationProviderOk();
                final boolean valueChanged = locationProviderOkAtomicBoolean
                        .compareAndSet(!newLocationProviderOkValue, newLocationProviderOkValue);
                if (valueChanged) {
                    observer.onNext(newLocationProviderOkValue);
                }
            }
        };
        observer.onSubscribe(Disposables.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                context.unregisterReceiver(broadcastReceiver);
            }
        }));
        observer.onNext(locationProviderOk);
        context.registerReceiver(broadcastReceiver, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
    }
}
