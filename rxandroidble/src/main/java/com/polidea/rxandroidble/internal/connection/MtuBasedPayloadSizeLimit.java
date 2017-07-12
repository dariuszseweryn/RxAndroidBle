package com.polidea.rxandroidble.internal.connection;


import android.support.annotation.RestrictTo;

import com.polidea.rxandroidble.RxBleConnection;

import javax.inject.Inject;
import javax.inject.Named;

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
