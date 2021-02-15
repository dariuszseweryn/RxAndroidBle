package com.polidea.rxandroidble3.internal.operations;


import androidx.annotation.RestrictTo;

import com.polidea.rxandroidble3.internal.Priority;
import com.polidea.rxandroidble3.internal.serialization.QueueReleaseInterface;

import io.reactivex.rxjava3.core.Observable;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Operation<T> extends Comparable<Operation<?>> {

    Observable<T> run(QueueReleaseInterface queueReleaseInterface);

    Priority definedPriority();
}
