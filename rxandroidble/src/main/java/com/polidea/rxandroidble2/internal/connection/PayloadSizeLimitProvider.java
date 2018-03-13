package com.polidea.rxandroidble2.internal.connection;


import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface PayloadSizeLimitProvider {

    int getPayloadSizeLimit();
}
