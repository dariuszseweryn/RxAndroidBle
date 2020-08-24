package com.polidea.rxandroidble2.internal.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble2.scan.ScanRecord;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Internal helper class for extracting list of Service UUIDs from Advertisement data
 *
 * @link http://stackoverflow.com/questions/31668791/how-can-i-read-uuids-from-advertisement-data-ios-overflow-area-in-android
 * @deprecated use {@link com.polidea.rxandroidble2.internal.util.ScanRecordParser} instead.
 * This class may change in later releases.
 */
@Deprecated
public class UUIDUtil {

    private ScanRecordParser parser;

    public UUIDUtil() {
        parser = new ScanRecordParser();
    }

    public List<UUID> extractUUIDs(byte[] scanResult) {
        return parser.extractUUIDs(scanResult);
    }

    @NonNull
    public Set<UUID> toDistinctSet(@Nullable UUID[] uuids) {
        if (uuids == null) uuids = new UUID[0];
        return new HashSet<>(Arrays.asList(uuids));
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public ScanRecord parseFromBytes(byte[] scanRecord) {
        return parser.parseFromBytes(scanRecord);
    }
}
