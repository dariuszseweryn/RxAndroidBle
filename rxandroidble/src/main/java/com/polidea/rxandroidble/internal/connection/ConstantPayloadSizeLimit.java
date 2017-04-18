package com.polidea.rxandroidble.internal.connection;


import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ConstantPayloadSizeLimit implements PayloadSizeLimitProvider {

    private final int limit;

    ConstantPayloadSizeLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public int getPayloadSizeLimit() {
        return limit;
    }
}
