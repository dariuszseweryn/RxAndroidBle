package com.polidea.rxandroidble.internal.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

/**
 * @link http://stackoverflow.com/questions/31668791/how-can-i-read-uuids-from-advertisement-data-ios-overflow-area-in-android
 */
public class UUIDUtil {

    @Inject
    UUIDUtil() {
    }

    public List<UUID> extractUUIDs(byte[] scanResult) {
        List<UUID> uuids = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(scanResult).order(ByteOrder.LITTLE_ENDIAN);

        while (buffer.remaining() > 2) {
            byte length = buffer.get();
            if (length == 0) break;

            byte type = buffer.get();
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2) {
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;

                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;

                default:
                    buffer.position(buffer.position() + length - 1);
                    break;
            }
        }

        return uuids;
    }

    @NonNull
    public Set<UUID> toDistinctSet(@Nullable UUID[] uuids) {
        if (uuids == null) uuids = new UUID[0];
        return new HashSet<>(Arrays.asList(uuids));
    }
}
