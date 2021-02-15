package com.polidea.rxandroidble2.internal.util;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.UUID;

public class ByteAssociation<T> {

    public final T first;
    public final byte[] second;

    public ByteAssociation(@NonNull T first, byte[] second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ByteAssociation)) {
            return false;
        }
        ByteAssociation<?> ba = (ByteAssociation<?>) o;
        return Arrays.equals(ba.second, second) && ba.first.equals(first);
    }

    @Override
    public int hashCode() {
        return first.hashCode() ^ Arrays.hashCode(second);
    }

    @Override
    public String toString() {
        String firstDescription;
        if (first instanceof BluetoothGattCharacteristic) {
            firstDescription = BluetoothGattCharacteristic.class.getSimpleName()
                    + "(" + ((BluetoothGattCharacteristic) first).getUuid().toString() + ")";
        } else if (first instanceof BluetoothGattDescriptor) {
            firstDescription = BluetoothGattDescriptor.class.getSimpleName()
                    + "(" + ((BluetoothGattDescriptor) first).getUuid().toString() + ")";
        } else if (first instanceof UUID) {
            firstDescription = UUID.class.getSimpleName()
                    + "(" + first.toString() + ")";
        } else {
            firstDescription = first.getClass().getSimpleName();
        }
        return getClass().getSimpleName() + "[first=" + firstDescription + ", second=" + Arrays.toString(second) + "]";
    }

    public static <T> ByteAssociation<T> create(T first, byte[] bytes) {
        return new ByteAssociation<>(first, bytes);
    }
}
