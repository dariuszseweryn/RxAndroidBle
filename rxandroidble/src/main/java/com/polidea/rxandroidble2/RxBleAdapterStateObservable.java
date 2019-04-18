package com.polidea.rxandroidble2;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.NonNull;

import javax.inject.Inject;
import com.polidea.rxandroidble2.internal.RxBleLog;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Action;

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
        public String toString() {
            return stateName;
        }
    }

    @NonNull
    private final Context context;

    @Inject
    public RxBleAdapterStateObservable(@NonNull final Context context) {
        this.context = context;
    }

    @Override
    protected void subscribeActual(final Observer<? super BleAdapterState> observer) {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    BleAdapterState internalState = mapToBleAdapterState(state);
                    RxBleLog.i("Adapter state changed: %s", internalState);
                    observer.onNext(internalState);
                }
            }
        };
        observer.onSubscribe(Disposables.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                try {
                    context.unregisterReceiver(receiver);
                } catch (IllegalArgumentException exception) {
                    RxBleLog.w("The receiver is already not registered.");
                }
            }
        }));
        context.registerReceiver(receiver, createFilter());
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
