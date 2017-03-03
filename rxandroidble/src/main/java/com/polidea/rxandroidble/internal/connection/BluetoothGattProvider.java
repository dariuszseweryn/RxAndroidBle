package com.polidea.rxandroidble.internal.connection;

import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

@ConnectionScope
public class BluetoothGattProvider {

    private final AtomicReference<BluetoothGatt> reference = new AtomicReference<>();

    @Inject
    BluetoothGattProvider() {
    }

    /**
     * Provides most recent instance of the BluetoothGatt. Keep in mind that the gatt may not be available, hence null will be returned.
     */
    public BluetoothGatt getBluetoothGatt() {
        return reference.get();
    }

    /**
     * Invalidates GATT storage.
     */
    public void invalidate() {
        reference.set(null);
    }

    /**
     * Updates GATT instance storage if it wasn't initialized previously.
     */
    public void updateBluetoothGatt(@NonNull BluetoothGatt bluetoothGatt) {
        reference.compareAndSet(null, bluetoothGatt);
    }
}
