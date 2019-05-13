package com.polidea.rxandroidble2.internal.util;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Build;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;
import com.polidea.rxandroidble2.ClientComponent;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.functions.Cancellable;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class LocationServicesOkObservableApi23Factory {
    private final Context context;
    private final LocationServicesStatus locationServicesStatus;
    private final Scheduler bluetoothInteractionScheduler;

    @Inject
    LocationServicesOkObservableApi23Factory(
            final Context context,
            final LocationServicesStatus locationServicesStatus,
            @Named(ClientComponent.NamedSchedulers.BLUETOOTH_INTERACTION) final Scheduler bluetoothInteractionScheduler) {
        this.context = context;
        this.locationServicesStatus = locationServicesStatus;
        this.bluetoothInteractionScheduler = bluetoothInteractionScheduler;
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
                .subscribeOn(bluetoothInteractionScheduler)
                .unsubscribeOn(bluetoothInteractionScheduler);
    }
}
