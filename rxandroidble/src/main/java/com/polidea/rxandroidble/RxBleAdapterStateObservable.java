package com.polidea.rxandroidble;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;

import javax.inject.Inject;

import rx.Emitter;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Cancellable;
import rx.internal.operators.OnSubscribeCreate;

/**
 * Observes Bluetooth adapter state. This responds to user interactions as well as system controlled state changes.
 * <p>
 * NOTE: Make sure that this Observable is unsubscribed according to the Activity lifecycle. It internally uses BroadcastReceiver, so
 * it is required that it us unregistered before onDestroy.
 */
public class RxBleAdapterStateObservable extends Observable<RxBleAdapterStateObservable.BleAdapterState> {

    public static class BleAdapterState {

        public static final BleAdapterState STATE_ON = new BleAdapterState(true);
        public static final BleAdapterState STATE_OFF = new BleAdapterState(false);
        public static final BleAdapterState STATE_TURNING_ON = new BleAdapterState(false);
        public static final BleAdapterState STATE_TURNING_OFF = new BleAdapterState(false);

        private final boolean isUsable;

        private BleAdapterState(boolean isUsable) {
            this.isUsable = isUsable;
        }

        public boolean isUsable() {
            return isUsable;
        }
    }

    @Inject
    public RxBleAdapterStateObservable(@NonNull final Context context) {
        super(new OnSubscribeCreate<>(
                new Action1<Emitter<BleAdapterState>>() {
                    @Override
                    public void call(final Emitter<BleAdapterState> emitter) {
                        final BroadcastReceiver receiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                String action = intent.getAction();

                                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                                    emitter.onNext(mapToBleAdapterState(state));
                                }
                            }
                        };
                        context.registerReceiver(receiver, createFilter());
                        emitter.setCancellation(new Cancellable() {
                            @Override
                            public void cancel() throws Exception {
                                context.unregisterReceiver(receiver);
                            }
                        });
                    }
                },
                Emitter.BackpressureMode.LATEST
        ));
    }

    private static BleAdapterState mapToBleAdapterState(int state) {

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

    private static IntentFilter createFilter() {
        return new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    }
}
