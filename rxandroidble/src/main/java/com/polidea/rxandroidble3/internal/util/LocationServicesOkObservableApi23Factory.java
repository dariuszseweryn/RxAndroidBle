package com.polidea.rxandroidble3.internal.util;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;

import bleshadow.javax.inject.Inject;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.functions.Cancellable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@TargetApi(19 /* Build.VERSION_CODES.KITKAT */)
public class LocationServicesOkObservableApi23Factory {
    final Context context;
    final LocationServicesStatus locationServicesStatus;

    @Inject
    LocationServicesOkObservableApi23Factory(
            final Context context,
            final LocationServicesStatus locationServicesStatus) {
        this.context = context;
        this.locationServicesStatus = locationServicesStatus;
    }

    public Observable<Boolean> get() {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(final ObservableEmitter<Boolean> emitter) {
                final boolean initialValue = locationServicesStatus.isLocationProviderOk();
                final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        final boolean newValue = locationServicesStatus.isLocationProviderOk();
                        emitter.onNext(newValue);
                    }
                };
                emitter.onNext(initialValue);
                context.registerReceiver(broadcastReceiver, new IntentFilter(LocationManager.MODE_CHANGED_ACTION));
                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() {
                        context.unregisterReceiver(broadcastReceiver);
                    }
                });
            }
        })
                .distinctUntilChanged()
                .subscribeOn(Schedulers.trampoline())
                .unsubscribeOn(Schedulers.trampoline());
    }
}
