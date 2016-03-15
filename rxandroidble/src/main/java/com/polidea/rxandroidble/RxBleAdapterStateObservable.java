package com.polidea.rxandroidble;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import rx.Observable;
import rx.Subscriber;
import rx.subscriptions.Subscriptions;

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

    public RxBleAdapterStateObservable(Context context) {
        super(subscriber -> onSubscribe(context.getApplicationContext(), subscriber));
    }

    private static void onSubscribe(Context context, final Subscriber<? super BleAdapterState> subscriber) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onStateBroadcastReceived(intent, subscriber);
            }
        };
        context.registerReceiver(receiver, createFilter());
        subscriber.add(Subscriptions.create(() -> context.unregisterReceiver(receiver)));
    }

    private static void onStateBroadcastReceived(Intent intent, Subscriber<? super BleAdapterState> subscriber) {
        String action = intent.getAction();

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            subscriber.onNext(mapToBleAdapterState(state));
        }
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
