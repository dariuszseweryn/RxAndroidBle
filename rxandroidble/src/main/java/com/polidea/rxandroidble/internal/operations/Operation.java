package com.polidea.rxandroidble.internal.operations;


import android.support.annotation.RestrictTo;
import com.polidea.rxandroidble.internal.Priority;
import com.polidea.rxandroidble.internal.RadioReleaseInterface;
import rx.Observable;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Operation<T> extends Comparable<Operation<?>> {

    Observable<T> run(RadioReleaseInterface radioReleaseInterface);

    Priority definedPriority();
}
