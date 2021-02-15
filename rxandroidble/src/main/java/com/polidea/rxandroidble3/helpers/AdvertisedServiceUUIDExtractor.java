package com.polidea.rxandroidble3.helpers;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.polidea.rxandroidble3.internal.util.ScanRecordParser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AdvertisedServiceUUIDExtractor {

    private final ScanRecordParser parser;

    public AdvertisedServiceUUIDExtractor() {
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
}
