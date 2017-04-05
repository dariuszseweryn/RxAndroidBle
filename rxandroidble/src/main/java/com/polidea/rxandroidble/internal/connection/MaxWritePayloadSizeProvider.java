package com.polidea.rxandroidble.internal.connection;


import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MaxWritePayloadSizeProvider implements IntProvider {

    private final IntProvider mtuProvider;
    private final int gattWriteMtuOverhead;

    MaxWritePayloadSizeProvider(IntProvider mtuProvider, int gattWriteMtuOverhead) {
        this.mtuProvider = mtuProvider;
        this.gattWriteMtuOverhead = gattWriteMtuOverhead;
    }

    @Override
    public int getValue() {
        return mtuProvider.getValue() - gattWriteMtuOverhead;
    }
}
