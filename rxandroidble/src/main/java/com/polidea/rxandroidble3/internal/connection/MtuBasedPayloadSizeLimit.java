package com.polidea.rxandroidble3.internal.connection;


import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble3.RxBleConnection;

import bleshadow.javax.inject.Inject;
import bleshadow.javax.inject.Named;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ConnectionScope
class MtuBasedPayloadSizeLimit implements PayloadSizeLimitProvider {

    private final RxBleConnection rxBleConnection;
    private final int gattWriteMtuOverhead;

    @Inject
    MtuBasedPayloadSizeLimit(RxBleConnection rxBleConnection,
                             @Named(ConnectionComponent.NamedInts.GATT_WRITE_MTU_OVERHEAD) int gattWriteMtuOverhead) {
        this.rxBleConnection = rxBleConnection;
        this.gattWriteMtuOverhead = gattWriteMtuOverhead;
    }

    @Override
    public int getPayloadSizeLimit() {
        return rxBleConnection.getMtu() - gattWriteMtuOverhead;
    }
}
