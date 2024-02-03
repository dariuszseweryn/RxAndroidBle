package com.polidea.rxandroidble2.internal.connection;


import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble2.RxBleConnection;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ConnectionScope
class MtuBasedPayloadSizeLimit implements PayloadSizeLimitProvider {

    private final RxBleConnection rxBleConnection;
    private final int gattWriteMtuOverhead;
    private final int maxAttributeLength;

    @Inject
    MtuBasedPayloadSizeLimit(RxBleConnection rxBleConnection,
                             @Named(ConnectionComponent.NamedInts.GATT_WRITE_MTU_OVERHEAD) int gattWriteMtuOverhead,
                             @Named(ConnectionComponent.NamedInts.GATT_MAX_ATTR_LENGTH) int maxAttributeLength) {
        this.rxBleConnection = rxBleConnection;
        this.gattWriteMtuOverhead = gattWriteMtuOverhead;
        this.maxAttributeLength = maxAttributeLength;
    }

    @Override
    public int getPayloadSizeLimit() {
        int maxWritePayloadForMtu = rxBleConnection.getMtu() - gattWriteMtuOverhead;
        // See https://issuetracker.google.com/issues/307234027}
        return Math.min(maxWritePayloadForMtu, maxAttributeLength);
    }
}
