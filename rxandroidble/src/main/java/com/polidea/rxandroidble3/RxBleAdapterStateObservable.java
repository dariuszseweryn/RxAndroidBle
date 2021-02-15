package com.polidea.rxandroidble2;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.NonNull;

import bleshadow.javax.inject.Inject;
import com.polidea.rxandroidble2.internal.RxBleLog;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.functions.Cancellable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Observes Bluetooth adapter state. This responds to user interactions as well as system controlled state changes.
 * <p>
 * NOTE: Make sure that this Observable is unsubscribed according to the Activity lifecycle. It internally uses BroadcastReceiver, so
 * it is required that it us unregistered before onDestroy.
 */
public class RxBleAdapterStateObservable extends Observable<RxBleAdapterStateObservable.BleAdapterState> {

    public static class BleAdapterState {

        public static final BleAdapterState STATE_ON = new BleAdapterState(true, "STATE_ON");
        public static final BleAdapterState STATE_OFF = new BleAdapterState(false, "STATE_OFF");
        public static final BleAdapterState STATE_TURNING_ON = new BleAdapterState(false, "STATE_TURNING_ON");
        public static final BleAdapterState STATE_TURNING_OFF = new BleAdapterState(false, "STATE_TURNING_OFF");

        private final boolean isUsable;
        private final String stateName;

        private BleAdapterState(boolean isUsable, String stateName) {
            this.isUsable = isUsable;
            this.stateName = stateName;
        }

        public boolean isUsable() {
            return isUsable;
        }

        @Override
        @NonNull
        public String toString() {
            return stateName;
        }
    }

    @NonNull
    private final Observable<BleAdapterState> bleAdapterStateObservable;

    @Inject
    public RxBleAdapterStateObservable(@NonNull final Context context) {
        bleAdapterStateObservable = Observable.create(new ObservableOnSubscribe<BleAdapterState>() {
            @Override
            public void subscribe(final ObservableEmitter<BleAdapterState> emitter) {
                final BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                        BleAdapterState internalState = mapToBleAdapterState(state);
                        RxBleLog.i("Adapter state changed: %s", internalState);
                        emitter.onNext(internalState);
                    }
                };
                context.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
                emitter.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() {
                        context.unregisterReceiver(receiver);
                    }
                });
            }
        })
                .subscribeOn(Schedulers.trampoline())
                .unsubscribeOn(Schedulers.trampoline())
                .share();
    }

    @Override
    protected void subscribeActual(final Observer<? super BleAdapterState> observer) {
        bleAdapterStateObservable.subscribe(observer);
    }

    static BleAdapterState mapToBleAdapterState(int state) {

        switch (state) {
            case BluetoothAdapter.STATE_ON:
                return BleAdapterState.STATE_ON;
            case BluetoothAdapter.STATE_TURNING_ON:
                return BleAdapterState.STATE_TURNING_ON;
            case BluetoothAdapter.STATE_TURNING_OFF:
                return BleAdapterState.STATE_TURNING_OFF;
            case BluetoothAdapter.STATE_OFF:
            default:
                return BleAdapterState.STATE_OFF;
        }
    }
}
