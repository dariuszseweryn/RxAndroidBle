package com.polidea.rxandroidble.internal.connection;


import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface PayloadSizeLimitProvider {

    int getPayloadSizeLimit();
}
