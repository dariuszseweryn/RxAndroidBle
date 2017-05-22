package com.polidea.rxandroidble.internal;

import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.internal.operations.Operation;
import rx.Observable;

public interface RxBleRadio {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    <T> Observable<T> queue(Operation<T> operation);
}
