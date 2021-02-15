package com.polidea.rxandroidble2.internal.connection;


import androidx.annotation.RestrictTo;

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
