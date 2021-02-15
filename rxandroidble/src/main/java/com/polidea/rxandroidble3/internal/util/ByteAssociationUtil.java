package com.polidea.rxandroidble3.internal.util;

import android.bluetooth.BluetoothGattDescriptor;

import java.util.UUID;

import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.functions.Predicate;

public class ByteAssociationUtil {

    private ByteAssociationUtil() {
    }

    public static Predicate<? super ByteAssociation<UUID>> characteristicUUIDPredicate(final UUID characteristicUUID) {
        return new Predicate<ByteAssociation<UUID>>() {
            @Override
            public boolean test(ByteAssociation<UUID> uuidPair) {
                return uuidPair.first.equals(characteristicUUID);
            }
        };
    }

    public static Function<ByteAssociation<?>, byte[]> getBytesFromAssociation() {
        return new Function<ByteAssociation<?>, byte[]>() {
            @Override
            public byte[] apply(ByteAssociation<?> byteAssociation) {
                return byteAssociation.second;
            }
        };
    }

    public static Predicate<? super ByteAssociation<BluetoothGattDescriptor>>
    descriptorPredicate(final BluetoothGattDescriptor bluetoothGattDescriptor) {
        return new Predicate<ByteAssociation<BluetoothGattDescriptor>>() {
            @Override
            public boolean test(ByteAssociation<BluetoothGattDescriptor> uuidPair) {
                return uuidPair.first.equals(bluetoothGattDescriptor);
            }
        };
    }
}
