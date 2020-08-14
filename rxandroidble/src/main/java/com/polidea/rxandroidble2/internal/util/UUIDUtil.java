package com.polidea.rxandroidble2.internal.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.polidea.rxandroidble2.internal.scan.ScanRecordImplCompat;
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
 * @deprecated use {@link ScanRecordImplCompat} instead.
 * This class may change in later releases.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class UUIDUtil {

    public UUIDUtil() {
    }

    public List<UUID> extractUUIDs(byte[] scanResult) {
        return ScanRecordImplCompat.extractUUIDs(scanResult);
    }

    @NonNull
    public Set<UUID> toDistinctSet(@Nullable UUID[] uuids) {
        if (uuids == null) uuids = new UUID[0];
        return new HashSet<>(Arrays.asList(uuids));
    }

    public static ScanRecord parseFromBytes(byte[] scanRecord) {
        return ScanRecordImplCompat.parseFromBytes(scanRecord);
    }
}
